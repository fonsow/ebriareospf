package engine_modules_helpers;

import java.util.LinkedList;

public class IOData {
    public static class IOString extends IOData {
        public String string;

        public IOString(String string) {
            this.string = string;
        }
    }

    public static class IOList extends IOData {
        public LinkedList<ModuleIO> list;

        public IOList(LinkedList<ModuleIO> list) {
            this.list = list;
        }
    }

    public static class IOHttpObj extends IOData {
        public HttpObject httpObject;

        public IOHttpObj(HttpObject httpObject) {
            this.httpObject = httpObject;
        }
    }

    public static class IOTuple extends IOData {
        public long number;
        public String string;

        public IOTuple(long number, String string) {
            this.number = number;
            this.string = string;
        }
    }
}
