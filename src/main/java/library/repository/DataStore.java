package library.repository;

import java.util.List;

public interface DataStore {
    List<List<String>> read(String collectionName);
    void write(String collectionName, List<List<String>> records);
}
