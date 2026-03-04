import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Date

// Git is needed in your system PATH for these commands to work.
// If it's not installed, you can return a random value as a workaround
fun Project.getCommitCount(): String {
    return try {
        runCommand("git", "rev-list", "--count", "HEAD")
    } catch (e: Exception) {
        "1"
    }
}

fun Project.getBetaCount(): String {
    return try {
        val betaTags = runCommand("git", "tag", "-l", "--sort=refname", "v${AndroidVersions.versionName}-b*")
        String.format("%02d", if (betaTags.isNotEmpty()) {
            val betaTag = betaTags.split("\n").last().substringAfter("-b").toIntOrNull()
            ((betaTag ?: 0) + 1)
        } else {
            1
        })
    } catch (e: Exception) {
        "01"
    }
}

fun Project.getGitSha(): String {
    return try {
        runCommand("git", "rev-parse", "--short", "HEAD")
    } catch (e: Exception) {
        "unknown"
    }
}

fun Project.getBuildTime(): String {
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
    df.timeZone = TimeZone.getTimeZone("UTC")
    return df.format(Date())
}

private fun runCommand(vararg command: String): String {
    val process = ProcessBuilder(*command)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    process.waitFor()
    return process.inputStream.bufferedReader().readText().trim()
}
