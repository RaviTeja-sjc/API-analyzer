package com.apianalyzer.analysis.application.service.ast;

import com.apianalyzer.analysis.domain.model.ast.AstModels.*;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class JavaAstParserService {

    public List<ClassInfo> parseSourceCode(String sourceCode, String filePath) {
        List<ClassInfo> classes = new ArrayList<>();
        if (sourceCode == null || sourceCode.isBlank()) {
            return classes;
        }

        try {
            CompilationUnit cu = StaticJavaParser.parse(sourceCode);
            String pkgName = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("default");

            List<String> imports = new ArrayList<>();
            cu.getImports().forEach(i -> imports.add(i.getNameAsString()));

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cid -> {
                int classStart = cid.getBegin().map(p -> p.line).orElse(1);
                int classEnd = cid.getEnd().map(p -> p.line).orElse(classStart);

                ClassInfo classInfo = ClassInfo.builder()
                        .packageName(pkgName)
                        .className(cid.getNameAsString())
                        .filePath(filePath)
                        .isInterface(cid.isInterface())
                        .startLine(classStart)
                        .endLine(classEnd)
                        .imports(new ArrayList<>(imports))
                        .build();

                // Class annotations and base path
                String classBasePath = "";
                for (AnnotationExpr a : cid.getAnnotations()) {
                    String aName = a.getNameAsString();
                    classInfo.getAnnotations().add(aName);
                    if ("RequestMapping".equals(aName) || aName.endsWith(".RequestMapping")) {
                        String pathVal = extractAnnotationPath(a);
                        if (pathVal != null) {
                            classBasePath = pathVal;
                        }
                    }
                }
                classInfo.setBasePath(classBasePath);

                // Extract Types (fields, extended classes, implemented interfaces)
                cid.getExtendedTypes().forEach(t -> classInfo.getTypeReferences().add(t.asString()));
                cid.getImplementedTypes().forEach(t -> classInfo.getTypeReferences().add(t.asString()));
                cid.getFields().forEach(f -> {
                    String typeStr = f.getCommonType().asString();
                    classInfo.getTypeReferences().add(typeStr);
                });

                // Extract Methods
                final String finalClassBasePath = classBasePath;
                cid.getMethods().forEach(md -> {
                    int mStart = md.getBegin().map(p -> p.line).orElse(classStart);
                    int mEnd = md.getEnd().map(p -> p.line).orElse(mStart);

                    MethodInfo methodInfo = MethodInfo.builder()
                            .name(md.getNameAsString())
                            .returnType(md.getTypeAsString())
                            .startLine(mStart)
                            .endLine(mEnd)
                            .build();

                    // Parse method parameters
                    md.getParameters().forEach(p -> methodInfo.getParameters().add(p.getNameAsString()));

                    // Check for Spring Web mapping annotations
                    String httpMethod = null;
                    String methodPath = "";

                    for (AnnotationExpr a : md.getAnnotations()) {
                        String aName = a.getNameAsString();
                        methodInfo.getAnnotations().add(aName);

                        if ("GetMapping".equals(aName) || aName.endsWith(".GetMapping")) {
                            httpMethod = "GET";
                            methodPath = extractAnnotationPath(a);
                        } else if ("PostMapping".equals(aName) || aName.endsWith(".PostMapping")) {
                            httpMethod = "POST";
                            methodPath = extractAnnotationPath(a);
                        } else if ("PutMapping".equals(aName) || aName.endsWith(".PutMapping")) {
                            httpMethod = "PUT";
                            methodPath = extractAnnotationPath(a);
                        } else if ("DeleteMapping".equals(aName) || aName.endsWith(".DeleteMapping")) {
                            httpMethod = "DELETE";
                            methodPath = extractAnnotationPath(a);
                        } else if ("PatchMapping".equals(aName) || aName.endsWith(".PatchMapping")) {
                            httpMethod = "PATCH";
                            methodPath = extractAnnotationPath(a);
                        } else if ("RequestMapping".equals(aName) || aName.endsWith(".RequestMapping")) {
                            httpMethod = extractRequestMappingMethod(a);
                            methodPath = extractAnnotationPath(a);
                        }
                    }

                    if (httpMethod != null) {
                        methodInfo.setHttpMethod(httpMethod);
                        methodInfo.setMethodPath(methodPath != null ? methodPath : "");
                        methodInfo.setFullEndpointPath(combinePaths(finalClassBasePath, methodPath));
                    }

                    // Extract Method Calls (downstream service calls, repository queries)
                    md.findAll(MethodCallExpr.class).forEach(mce -> {
                        MethodCallInfo call = MethodCallInfo.builder()
                                .name(mce.getNameAsString())
                                .scope(mce.getScope().map(s -> s.toString()).orElse("this"))
                                .build();
                        mce.getArguments().forEach(arg -> call.getArguments().add(arg.toString()));
                        methodInfo.getMethodCalls().add(call);
                    });

                    classInfo.getMethods().add(methodInfo);
                });

                classes.add(classInfo);
            });
        } catch (Exception e) {
            // Log parse failure safely without crashing pipeline
            throw new IllegalArgumentException("Failed to parse Java source: " + e.getMessage());
        }
        return classes;
    }

    private String extractAnnotationPath(AnnotationExpr a) {
        if (a instanceof SingleMemberAnnotationExpr s) {
            return stripQuotes(s.getMemberValue().toString());
        } else if (a instanceof NormalAnnotationExpr n) {
            for (MemberValuePair pair : n.getPairs()) {
                String name = pair.getNameAsString();
                if ("value".equals(name) || "path".equals(name)) {
                    return stripQuotes(pair.getValue().toString());
                }
            }
        }
        return "";
    }

    private String extractRequestMappingMethod(AnnotationExpr a) {
        if (a instanceof NormalAnnotationExpr n) {
            for (MemberValuePair pair : n.getPairs()) {
                if ("method".equals(pair.getNameAsString())) {
                    String val = pair.getValue().toString().toUpperCase();
                    if (val.contains("GET")) return "GET";
                    if (val.contains("POST")) return "POST";
                    if (val.contains("PUT")) return "PUT";
                    if (val.contains("DELETE")) return "DELETE";
                    if (val.contains("PATCH")) return "PATCH";
                }
            }
        }
        return "GET"; // default for @RequestMapping without method
    }

    private String stripQuotes(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private String combinePaths(String base, String sub) {
        if (base == null) base = "";
        if (sub == null) sub = "";
        base = base.trim();
        sub = sub.trim();

        if (base.isEmpty()) return sub.isEmpty() ? "/" : (sub.startsWith("/") ? sub : "/" + sub);
        if (sub.isEmpty()) return base.startsWith("/") ? base : "/" + base;

        if (!base.startsWith("/")) base = "/" + base;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!sub.startsWith("/")) sub = "/" + sub;

        return base + sub;
    }
}
