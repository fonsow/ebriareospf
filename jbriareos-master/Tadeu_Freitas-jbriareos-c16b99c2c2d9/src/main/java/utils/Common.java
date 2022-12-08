package utils;

public class Common {
    public static final String JHC_CONFIG_PATH = "./src/config/jhc.json";

    public static final String JMS_CONFIG_PATH = "./src/config/jms.json";
    public static final String ZCLIENT_CONFIG_PATH = "./src/config/client.json";
    public static final String ZWORKER_CONFIG_PATH = "./src/config/worker.json";
    public static final String ZBROKER_CONFIG_PATH = "./src/config/broker.json";
    public static final String ZCLUSTER_CONFIG_PATH = "./src/config/cluster.json";

    public static final String PIPELINES_ROOT_PATH = "./src/config/engine_pipelines/";
    public static final String SIGNATURES_PATH = "./src/config/signatures.txt";

    public static final String CLIENT_CERT_FOLDER = "./.clientcert";
    public static final String CLUSTER_CERT_FOLDER = "./.clustercert";
    public static final String SERVER_CERT_FOLDER = "./.servercert";

    public static final String DOCKER_IMAGE_BUILDER = "./build-zworker-docker-image.sh";

    public enum PipelineType {
        InputPipeline,
        OutputPipeline,
    }

    public enum ProcessingMode {
        InlineLocal,
        Distributed,
    }

    public enum InputMode {
        SingleInput,
        MultipleInput
    }

    public enum Verdict {
        Accept,
        Drop
    }
}
