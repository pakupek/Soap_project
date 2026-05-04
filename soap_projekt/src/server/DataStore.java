package server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import client.RepairRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataStore {

    private static final String FILE = "requests.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<RepairRequest> load() {
        try {
            File file = new File(FILE);

            if (!file.exists()) return new ArrayList<>();

            return mapper.readValue(file, new TypeReference<List<RepairRequest>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void save(List<RepairRequest> list) {
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(FILE), list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}