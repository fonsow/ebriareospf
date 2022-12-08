package engine_modules;

import core.JMSInterface;
import engine_modules_helpers.IOData;
import engine_modules_helpers.IOType;
import engine_modules_helpers.ModuleIO;
import engine_modules_helpers.ModuleType;
import org.apache.commons.lang.ArrayUtils;
import utils.BPacket;
import utils.Common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Stream;

public class LeakDetector extends EngineModule {
    public LeakDetector() {
        super(ModuleType.LeakDetector);
    }

    @Override
    public IOType getInputType() {
        return IOType.Tuple;
    }

    @Override
    public IOType getOutputType() {
        return IOType.Tuple;
    }

    @Override
    public Common.InputMode getInputMode() {
        return Common.InputMode.SingleInput;
    }

    private static final int MIN_LEAK_LENGTH = 2;
    private static final int REPLACE = 0;
    private static final int BLOCK = 1;
    private static final int ALLOW_EXPLOIT = 2;
    private static final int MODE = BLOCK;


    private static class MemMap {
        int[] addressRange;
        String pattern;
        String permissions;
        String name;
        String[] addressRangeHex;

        public MemMap(int[] addressRange, String pattern, String permissions, String name, String[] addressRangeHex) {
            this.addressRange = addressRange;
            this.pattern = pattern;
            this.permissions = permissions;
            this.name = name;
            this.addressRangeHex = addressRangeHex;
        }
    }

    private String getPattern(String a, String b) {
        StringBuilder pattern = new StringBuilder();
        a = a.replace("\\x00", "");
        b = b.replace("\\x00", "");

        int minLength = Math.min(a.length(), b.length());
        for (int i  = 0; i < minLength; i++) {
            if (a.charAt(i) == b.charAt(i))
                pattern.append(a.charAt(i));
            else break;
        }

        return pattern.toString();
    }

    private LinkedList<MemMap> getMemoryMaps(long pid) {
        LinkedList<MemMap> maps = new LinkedList<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/" + pid + "/maps"));
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                String[] l = line.split(" ");

                String[] lAddr = l[0].split("-");
                int[] addressRange = {Integer.parseInt(lAddr[0],16), Integer.parseInt(lAddr[1],16)};
                String[] addressRangeHex = {Integer.toHexString(addressRange[0]), Integer.toHexString(addressRange[1])};

                ByteBuffer buf = ByteBuffer.allocate(4);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                buf.putInt(addressRange[0]);
                byte[] patStartAddr = buf.array();
                ArrayUtils.reverse(patStartAddr);

                ByteBuffer buf1 = ByteBuffer.allocate(4);
                buf1.order(ByteOrder.LITTLE_ENDIAN);
                buf1.putInt(addressRange[1]);
                byte[] patEndAddr = buf1.array();
                ArrayUtils.reverse(patEndAddr);

                String pattern = getPattern(Arrays.toString(patStartAddr), Arrays.toString(patEndAddr));
                if (pattern.length() >= MIN_LEAK_LENGTH) {
                    String permissions = l[1];
                    String name = "Unknown";
                    if (l.length == 6)
                        name = l[5];

                    maps.addLast(new MemMap(addressRange, pattern, permissions, name, addressRangeHex));
                }

                line = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return maps;
    }

    @Override
    public ModuleIO process(BPacket packet, ModuleIO data, JMSInterface jmsInterface) {
        if (data.ioType != IOType.Tuple) {
            System.out.println("-- LeakDetector --");
            System.out.println("Data's type is not the same as the data type needed by the module");
            System.exit(-1);
        }

        long pid = ((IOData.IOTuple) data.ioData).number;

        String packetData = packet.getApplicationData();
        if (packetData.isEmpty()) {
            packet.accept();
            return data;
        }

        Optional<ProcessHandle> obj = ProcessHandle.of(pid);
        if (!obj.isPresent()) {
            packet.accept();
            return data;
        }

        ProcessHandle process = obj.get();
        Stream<ProcessHandle> children = process.children();

        Stream<ProcessHandle> processStream = Stream.of(process);
        Iterator<ProcessHandle> iterator = Stream.concat(processStream, children).iterator();
        while (iterator.hasNext()) {
            ProcessHandle ph = iterator.next();
            LinkedList<MemMap> maps = getMemoryMaps(ph.pid());

            char[] packetDataArray = packetData.toCharArray();
            ArrayUtils.reverse(packetDataArray);

            String reversePacketData = Arrays.toString(packetDataArray);
            for (MemMap memMap : maps) {
                if (reversePacketData.contains(memMap.pattern)) {
                    if (MODE == BLOCK) {
                        System.out.println("Leak detected " + memMap.name +
                                " (" + Arrays.toString(memMap.addressRangeHex) + ") " +
                                "-> Dropping packet");
                        packet.drop();
                        return data;
                    } else if (MODE == REPLACE) {
                        System.out.println("Leak detected " + memMap.name + " " +
                                "-> Replacing leak data");
                        return data;
                    } else {
                        System.out.println("Leak detected " + memMap.name + " " +
                                "-> Waiting for exploit to complete");
                        break;
                    }
                }
            }
        }

        return data;
    }
}
