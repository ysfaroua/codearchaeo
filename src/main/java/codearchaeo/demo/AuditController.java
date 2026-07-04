package codearchaeo.demo;

import org.eclipse.jgit.api.Git;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AuditController {

    @PostMapping("/analyze")
    public Map<String, Object> analyze(@RequestParam String repoUrl) throws Exception {
        Path tempDir = Files.createTempDirectory("repo-");

        try (Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(tempDir.toFile())
                .call()) {

            List<Map<String, Object>> files = new ArrayList<>();

            Files.walk(tempDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    try {
                        List<String> lines = Files.readAllLines(p);
                        int lineCount = lines.size();
                        int complexity = computeComplexity(lines);
                        String relativePath = tempDir.relativize(p).toString();

                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("path", relativePath);
                        fileInfo.put("lines", lineCount);
                        fileInfo.put("complexity", complexity);
                        files.add(fileInfo);

                    } catch (IOException e) {
                        // skip unreadable files
                    }
                });

            return Map.of(
                "repo", repoUrl,
                "fileCount", files.size(),
                "files", files
            );
        }
    }

    private int computeComplexity(List<String> lines) {
        int score = 1; // base complexity
        List<String> keywords = List.of("if ", "else if ", "for ", "while ", "catch ", "switch ", "case ");
        for (String line : lines) {
            String trimmed = line.trim();
            for (String keyword : keywords) {
                if (trimmed.contains(keyword)) {
                    score++;
                    break; // count max 1 per line
                }
            }
        }
        return score;
    }
}