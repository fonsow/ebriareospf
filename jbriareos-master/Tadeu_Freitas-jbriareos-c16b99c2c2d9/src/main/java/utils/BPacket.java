package utils;

import nemo.it.unipr.netsec.ipstack.ip4.Ip4Packet;
import nemo.it.unipr.netsec.ipstack.ip4.IpAddress;
import nemo.it.unipr.netsec.ipstack.ip4.SocketAddress;
import nemo.it.unipr.netsec.ipstack.net.DataPacket;
import nemo.it.unipr.netsec.ipstack.tcp.TcpPacket;
import nemo.it.unipr.netsec.ipstack.udp.UdpPacket;
import org.json.simple.JSONObject;

public class BPacket {
    public DataPacket pkt;
    public Common.Verdict verdict;
    public boolean isFinalVerdict;
    public boolean newPayload;
    public byte[] payload;
    boolean parsed;
    public boolean tcpPacket;

    @Override
    public String toString() {
        return "pkt: '" + this.pkt.toString() + "'\n" +
                "verdict: '" + this.verdict.toString() + "'\n" +
                "isFinalVerdict: '" + this.isFinalVerdict + "'\n" +
                "payload: '" + new String(this.pkt.getPayload()) + "'\n";
    }

    public BPacket(byte[] payload) throws Exception {
        this.isFinalVerdict = false;
        this.verdict = Common.Verdict.Drop;
        this.payload = payload;
        this.parsed = false;
        this.newPayload = true;

        this.parsePkt();
    }

    public BPacket(byte[] payload, Common.Verdict verdict) {
        this.pkt = null;
        this.isFinalVerdict = false;
        this.verdict = verdict;
        this.payload = payload;
        this.parsed = false;
        this.newPayload = false;
    }

    public void accept() {
        this.isFinalVerdict = true;
        this.verdict = Common.Verdict.Accept;
    }

    public void drop() {
        this.isFinalVerdict = true;
        this.verdict = Common.Verdict.Drop;
    }

    void parsePkt() throws Exception {
        try {
            this.pkt = TcpPacket.parseTcpPacket(Ip4Packet.parseIp4Packet(this.payload));
            this.parsed = true;
            this.tcpPacket = true;
        } catch (Exception ignored) {
            try {
                this.pkt = UdpPacket.parseUdpPacket(Ip4Packet.parseIp4Packet(this.payload));
                this.parsed = true;
                this.tcpPacket = false;
            } catch (Exception ignored2) {
                System.out.println("Captured packet is neither TCP nor UDP");
                throw new Exception();
            }
        }
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
        this.newPayload = true;
    }

    public String getApplicationData() {
        if (!this.parsed)
            try {
                this.parsePkt();
            } catch (Exception ignored) {
                return "";
            }

        return new String(this.pkt.getPayload());
    }

    public String getProtocol() {
        if (this.tcpPacket)
            return "tcp";
        else
            return "udp";
    }

    public String getSrcIp() {
        if (!this.parsed)
            try {
                this.parsePkt();
            } catch (Exception ignored) {
                return "";
            }

        return ((IpAddress)this.pkt.getSourceAddress()).toInetAddress().getHostAddress();
    }

    public int getSrcPort() {
        if (!this.parsed)
            try {
                this.parsePkt();
            } catch (Exception ignored) {
                return -1;
            }

        return ((SocketAddress)this.pkt.getSourceAddress()).getPort();
    }

    public String getDstIP() {
        if (!this.parsed)
            try {
                this.parsePkt();
            } catch (Exception ignored) {
                return "";
            }

        return ((IpAddress)this.pkt.getDestAddress()).toInetAddress().getHostAddress();
    }

    public int getDstPort() {
        if (!this.parsed)
            try {
                this.parsePkt();
            } catch (Exception ignored) {
                return -1;
            }

        return ((SocketAddress)this.pkt.getDestAddress()).getPort();
    }

    public void blockIPAddress() {
        JSONObject ruleJSON = IpTables.formJSONfromRuleArgs(
                null,"INPUT", null, null, this.getSrcIp(), null,
                null, null, "DROP");

        try {
            IpTables.createNewRule(IpTables.formRuleFromJSON(ruleJSON));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Couldn't create the new rule.");
        }
    }

    public void redirectPackages(String redirDst) {
        String action = "DNAT --to-destination " + redirDst;

        JSONObject ruleJSON = IpTables.formJSONfromRuleArgs(
            "nat","PREROUTING", null, null, this.getSrcIp(), null,
                null, null, action);

        try {
            IpTables.createNewRule(IpTables.formRuleFromJSON(ruleJSON));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Couldn't create the new rule.");
        }
    }

    public boolean isNewConnection() {
        if (this.tcpPacket)
            return ((TcpPacket) this.pkt).hasSyn();

        System.out.println("Trying to check new connection on non-TCP packet");
        return true;
    }

    public boolean isConnectionClosed() {
        if (this.tcpPacket)
            return ((TcpPacket) this.pkt).hasFin();

        System.out.println("Trying to check if a connection is closed on non-TCP packet");
        return true;
    }
}