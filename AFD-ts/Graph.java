import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
    int BestSolution;
    List<Integer> vertices;
    Map<Integer, Integer> vMap;
    List<List<Integer>> adjacentList;
    int [][] G;

    public Graph(int bestSolution) {
        vertices = new ArrayList<>();
        vMap = new HashMap<>();
        adjacentList = new ArrayList<>();
        BestSolution=bestSolution;
    }
    public void creat(int vNum){
        G=new int[vNum][vNum];
    }
    public int getIdOrAddV(int name) {
        if (vMap.containsKey(name)) {
            return vMap.get(name);
        }
        int id = vertices.size();
        vMap.put(name, id);
        vertices.add(name);
        adjacentList.add(new ArrayList<>());
        return id;
    }
    public void addEdge(int source, int target) {
        adjacentList.get(source).add(target);
        adjacentList.get(target).add(source);
    }
}
