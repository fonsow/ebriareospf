package core;

import engines.Engine;
import nemo.it.unipr.netsec.ipstack.ip4.Ip4Packet;
import nemo.it.unipr.netsec.netfilter.NetfilterQueue;
import utils.BPacket;
import utils.Common;
import utils.IpTables;

import java.util.LinkedList;


public class Interceptor {
    Engine engine;
    int currentQueueId;
    LinkedList<NetfilterQueue> nfQueues;

    public static final Integer MAX_QUEUE_ID = 65535;

    public Interceptor(Engine engine) {
        this.engine = engine;
        this.currentQueueId = 0;
        this.nfQueues = new LinkedList<>();
    }

    private int getCurrentQueueId() {
        if (this.currentQueueId > MAX_QUEUE_ID)
            return -1;

        int currentQueue = this.currentQueueId;
        this.currentQueueId++;
        return currentQueue;
    }

    private void configureIpTables() {
        System.out.println("Configuring iptables");

        for (Pipeline pipeline : this.engine.pipelineList) {
            int queueId = this.getCurrentQueueId();

            if (queueId == -1) {
                System.out.println("Maximum number of queues reached");
                return;
            }

            String chain = IpTables.INPUT_CHAIN;
            if (pipeline.pipelineType.equals(Common.PipelineType.OutputPipeline))
                chain = IpTables.OUTPUT_CHAIN;

            if (IpTables.createNFQueueRule(queueId, chain, pipeline.port, pipeline.protocol,
                    pipeline.itf, pipeline.srcIp))
                this.engine.queueMap.put(queueId, pipeline);
            else
                System.out.println("Error configuring iptables for pipeline: " + pipeline.name);
        }
    }

    private void bindQueues() {
        System.out.println("Binding queues...");

        for (int queueId : this.engine.queueMap.keySet()) {
            NetfilterQueue nfQueue = new NetfilterQueue(queueId, (buf, len) -> {
                /*
                // JBriareos only accepts IPv4 packets, for now
                // This should be used, just to make sure, but the performance slightly drops
                Ip4Packet packet;
                try {
                    packet = Ip4Packet.parseIp4Packet(buf);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Caught packet is not IPv4");
                    return 0;
                }
                 */

                Engine.ProcessResult result = engine.process(buf, String.valueOf(queueId));
                BPacket bPacket = result.packet;
                Common.Verdict verdict = result.verdict;

                if (bPacket != null) {
                    if (bPacket.verdict.equals(Common.Verdict.Accept)) {
                        if (bPacket.newPayload) {
                            Ip4Packet packet = Ip4Packet.parseIp4Packet(buf);
                            packet.setPayload(bPacket.payload);

                            return packet.getPacketLength();
                        }

                        return len;
                    }

                    return 0;
                }

                if (verdict.equals(Common.Verdict.Accept))
                    return len;

                return 0;
            });

            new Thread(nfQueue::start).start();

            this.nfQueues.addLast(nfQueue);
        }

        System.out.println("Queues bound: " + this.nfQueues.size());
    }

    public void start() {
        this.engine.start();

        System.out.println("Starting Interceptor");
        this.configureIpTables();
        this.bindQueues();
        System.out.println("Interceptor is running");
    }

    public void stop() {
        System.out.println("Stopping the interceptor...");
        IpTables.cleanRules();
        for (NetfilterQueue queue : this.nfQueues) {
            queue.stop();
        }

        this.engine.stop();
    }
}