package core;

import components.ZClient;
import org.jctools.queues.MpscLinkedQueue;

import java.util.UUID;

public class JDSInterface implements Runnable {
    private static class QueueObject {
        byte[] data;
        String pipelineName;

        QueueObject(byte[] data, String pipelineName) {
            this.data = data;
            this.pipelineName = pipelineName;
        }
    }

    ZClient client;
    MpscLinkedQueue<QueueObject> queue;

    public JDSInterface(UUID id) {
        this.client = new ZClient(id);
        this.queue = new MpscLinkedQueue<>();
    }

    public void start() {
        System.out.println("Starting JDSInterface");

        this.client.start();
        new Thread(this).start();

        System.out.println("JDSInterface is ready");
    }

    public void stop() {
        System.out.println("Stopping JDSInterface...");
        this.client.stop();
    }

    public void process(byte[] data, String pipelineName) {
        this.queue.offer(new QueueObject(data, pipelineName));
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                QueueObject obj = this.queue.poll();
                if (obj != null)
                    this.client.process(obj.data, obj.pipelineName);
            } catch (RuntimeException ignored) {
                return;
            }
        }
    }
}
