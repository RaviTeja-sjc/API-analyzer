package com.apianalyzer.analysis.application.service.ast;
import com.apianalyzer.analysis.domain.model.ast.AstModels.ClassInfo;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class JavaAstParserServiceTest {

    @Test
    void testParseController() {
        JavaAstParserService service = new JavaAstParserService();
        String source = "package com.example;\n" +
                        "import org.springframework.web.bind.annotation.*;\n" +
                        "@RestController\n" +
                        "public class UserController {\n" +
                        "    @GetMapping(\"/users\")\n" +
                        "    public String getUsers() { return \"Users\"; }\n" +
                        "}";
                        
        List<ClassInfo> classes = service.parseSourceCode(source, "UserController.java");
        
        assertEquals(1, classes.size());
        ClassInfo clazz = classes.get(0);
        assertEquals("UserController", clazz.getClassName());
        assertTrue(clazz.isController());
        
        assertEquals(1, clazz.getMethods().size());
        assertEquals("getUsers", clazz.getMethods().get(0).getName());
        assertTrue(clazz.getMethods().get(0).getAnnotations().contains("GetMapping"));
    }
}
