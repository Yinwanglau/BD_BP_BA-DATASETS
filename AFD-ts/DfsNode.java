import java.util.ArrayList;
import java.util.List;

class DfsNode {
    int vId;
    int depth;
    DfsNode parent;
    List<DfsNode> children;
    DfsNode toLowest;
    boolean refreshed;
    InSet low;
    int AllChiNum;
    DfsNode(int id) {
        vId = id;
        parent = null;
        children = new ArrayList<>();
        depth = -1;
        toLowest = null;
        refreshed = false;
        AllChiNum=1;
    }
}