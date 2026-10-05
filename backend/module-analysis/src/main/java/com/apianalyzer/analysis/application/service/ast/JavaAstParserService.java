package com.apianalyzer.analysis.application.service.ast;
import com.apianalyzer.analysis.domain.model.ast.AstModels.*;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
@Service
public class JavaAstParserService {
    
    public List<ClassInfo> parseSourceCode(String sourceCode) {
        List<ClassInfo> classes = new ArrayList<>();
        try {
            CompilationUnit cu = StaticJavaParser.parse(sourceCode);
            String pkgName = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("default");
            
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cid -> {
                ClassInfo classInfo = ClassInfo.builder()
                        .packageName(pkgName)
                        .className(cid.getNameAsString())
                        .isInterface(cid.isInterface())
                        .build();
                        
                cid.getAnnotations().forEach(a -> classInfo.getAnnotations().add(a.getNameAsString()));
                
                // Extract Types (e.g. fields, extended classes)
                cid.getExtendedTypes().forEach(t -> classInfo.getTypeReferences().add(t.asString()));
                cid.getImplementedTypes().forEach(t -> classInfo.getTypeReferences().add(t.asString()));
                cid.getFields().forEach(f -> classInfo.getTypeReferences().add(f.getCommonType().asString()));
                
                // Extract Methods
                cid.getMethods().forEach(md -> {
                    MethodInfo methodInfo = MethodInfo.builder()
                            .name(md.getNameAsString())
                            .returnType(md.getTypeAsString())
                            .build();
                    md.getAnnotations().forEach(a -> methodInfo.getAnnotations().add(a.getNameAsString()));
                    
                    // Extract Method Calls (API clients, RestTemplate calls, etc)
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
            throw new IllegalArgumentException("Failed to parse Java source: " + e.getMessage());
        }
        return classes;
    }
}
