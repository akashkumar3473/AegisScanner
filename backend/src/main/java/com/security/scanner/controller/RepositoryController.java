package com.security.scanner.controller;

import com.security.scanner.model.Repository;
import com.security.scanner.model.User;
import com.security.scanner.repository.RepositoryRepository;
import com.security.scanner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Repository>> getRepositories(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(repositoryRepository.findByUserId(user.getId()));
    }

    @PostMapping
    public ResponseEntity<?> addRepository(@RequestBody Repository repo, Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        if (repo.getGitUrl() == null || repo.getGitUrl().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Git URL is required");
        }

        if (repo.getName() == null || repo.getName().trim().isEmpty()) {
            String url = repo.getGitUrl();
            String inferredName = url.substring(url.lastIndexOf("/") + 1).replace(".git", "");
            repo.setName(inferredName);
        }

        repo.setUser(user);
        Repository saved = repositoryRepository.save(repo);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRepository(@PathVariable Long id, Principal principal) {
        Repository repo = repositoryRepository.findById(id).orElse(null);
        if (repo == null) {
            return ResponseEntity.notFound().build();
        }

        if (!repo.getUser().getEmail().equals(principal.getName())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        repositoryRepository.delete(repo);
        return ResponseEntity.ok("Repository deleted successfully");
    }
}
