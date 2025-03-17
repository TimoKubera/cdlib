package de.deutschepost.sdm.cdlib.names.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

object GitRepository {
    fun lastBranchCommit(repoPath: String): GitRevision {
        val repository = getRepository(repoPath)
        val git = Git(repository)
        val logs = git.log().setMaxCount(1).call()
        val refCommit = logs.first()
        return GitRevision.of(refCommit)
    }

    fun getRemoteUrl(repoPath: String): String {
        val repository = getRepository(repoPath)
        return repository.config.getString("remote", "origin", "url").removeGitCredentials()
    }

    private fun getRepository(repoPath: String): Repository {
        val file = File(repoPath)
        return FileRepositoryBuilder().findGitDir(file).setMustExist(true).build()
    }

    private fun String.removeGitCredentials(): String = if (contains("@")) {
        "https://${this.substringAfter("@")}"
    } else {
        this
    }
}
