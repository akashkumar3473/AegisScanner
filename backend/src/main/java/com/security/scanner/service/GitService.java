package com.security.scanner.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Service
public class GitService {

    public void cloneRepository(String gitUrl, String branch, File targetDir) throws GitAPIException {
        if (targetDir.exists()) {
            deleteDirectory(targetDir);
        }
        targetDir.mkdirs();

        Git.cloneRepository()
                .setURI(gitUrl)
                .setDirectory(targetDir)
                .setBranch(branch != null && !branch.isEmpty() ? branch : "refs/heads/main")
                .setCloneAllBranches(false)
                .call()
                .close();
    }

    public void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        try {
            Files.walk(dir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            // Log directory deletion failure
            System.err.println("Failed to delete temp repository directory: " + e.getMessage());
        }
    }
}
