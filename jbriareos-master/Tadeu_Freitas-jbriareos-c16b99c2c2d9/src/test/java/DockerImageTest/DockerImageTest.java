package DockerImageTest;

import java.io.IOException;

public class DockerImageTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", "./build-zworker-docker-image.sh");

        long start = System.currentTimeMillis();
        System.out.println("Building zworker docker image...");
        Process process = processBuilder.start();
        process.waitFor();
        long end = System.currentTimeMillis();

        System.out.print("Image created in ");
        System.out.println(end - start + "ms");
    }
}
