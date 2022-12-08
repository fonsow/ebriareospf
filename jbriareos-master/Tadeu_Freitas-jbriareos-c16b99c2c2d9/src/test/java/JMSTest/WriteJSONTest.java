package JMSTest;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;

public class WriteJSONTest {
    static JSONObject object = new JSONObject();
    static JSONArray array = new JSONArray();


    @SuppressWarnings("unchecked")
    private static void initTest() {
        object.put("chain", "INPUT");
        object.put("protocol", "udp");
        object.put("action", "DROP");

        array.add("simple_test");
        array.add("is_this_ordered?");
        array.add("no");
        array.add("good");
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException, ParseException {
        initTest();

        System.out.println(object.toJSONString());
        System.out.println(array.toJSONString());

        RandomAccessFile file = new RandomAccessFile("./src/config/rules.json", "rw");
        FileLock lock = file.getChannel().lock();

        InputStream is = Channels.newInputStream(file.getChannel());

        JSONParser parser = new JSONParser();
        JSONArray array = (JSONArray) parser.parse(new InputStreamReader(is, StandardCharsets.UTF_8));

        array.add(object);

        file.setLength(0);
        file.write(array.toJSONString().getBytes());
        lock.release();
    }
}
