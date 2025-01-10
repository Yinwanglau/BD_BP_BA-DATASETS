import java.util.*;
import java.io.File;
class DynamicUpdate {
    List<Boolean> vIsOpen;
    InSet Cut;
    List<DfsNode> dfsNodes;
    Graph graph;
    DfsNode root;
    DfsNode removedV;
    DfsNode node;
    DfsNode oldRoot;
    DfsNode lowestConnection;
    Pair pair;
    DfsNode uNode;
    DfsNode backConnection;
    DfsNode lowestConnectionNode;
    DfsNode newTreeRoot;
    DfsNode curr;
    List<Integer> neighborsId;
    ArrayList<Integer> connections;
    int subRootId;
    ArrayList<Integer> subSubRoots;
    DfsNode vNode;
    DfsNode lowNode;
    DfsNode currNode;
    DfsNode addedNode;
    DfsNode subSubR;
    int currId;
    DfsNode subRoot;
    InSet vOnPathToRoot;
    List<DfsNode> children;
    Stack<DfsNode> nodeStack;
    public DynamicUpdate(String instance) throws Exception{
        Scanner scanner = new Scanner(new File(instance));
        //int BestSolution = scanner.nextInt();//记得改变算例第一行为此算例最佳解
        int vNum = scanner.nextInt();
        int eNum = scanner.nextInt();
        vIsOpen = new ArrayList<>(vNum);
        Cut = new InSet(vNum);
        dfsNodes = new ArrayList<>(vNum);
        nodeStack=new Stack<>();
        vOnPathToRoot=new InSet(vNum);
        connections=new ArrayList<>();
        subSubRoots=new ArrayList<>();
        graph = new Graph(0);
        for (int i = 0; i < eNum; i++) {
            int source = scanner.nextInt();
            int target = scanner.nextInt();
            graph.addEdge(graph.getIdOrAddV(source), graph.getIdOrAddV(target));
        }
        for (int i = 0; i < vNum; i++) {
            vIsOpen.add(true);
            dfsNodes.add(new DfsNode(i));
            dfsNodes.get(i).low=new InSet(vNum);
        }
        findAllCuts();
    }
    public void findAllCuts() {
        resetDfsNodes();
        int rootId = 0;
        while (!vIsOpen.get(rootId)) rootId++;
        root = dfsNodes.get(rootId);
        root.depth = 1;
        root.parent = null;
        buildDfsTreeRecur(root);
        if (root.children.size() > 1) {
            Cut.add(rootId);
        }
    }
    public void resetDfsNodes() {
        for (int i = 0; i < dfsNodes.size(); i++) {
            DfsNode node = dfsNodes.get(i);
            node.low.clear();
            node.parent = null;
            node.children.clear();
            node.depth = -1;
            node.toLowest = null;
            node.refreshed = false;
            Cut.remove(i);
        }
    }
    public void buildDfsTreeRecur(DfsNode node) {
        node.toLowest = node;
        node.refreshed = true;
        node.AllChiNum=1;
        for (int uId : graph.adjacentList.get(node.vId)) {
            if (!vIsOpen.get(uId)) continue;
            DfsNode uNode = dfsNodes.get(uId);
            if (uNode.depth == -1) {
                uNode.depth = node.depth + 1;
                uNode.parent = node;
                node.children.add(uNode);
                buildDfsTreeRecur(uNode);
                if (node.toLowest.depth > uNode.toLowest.depth) {
                    node.toLowest = uNode.toLowest;
                }
                if (uNode.toLowest.depth >= node.depth && node != root) {
                    Cut.add(node.vId);
                }
            } else {
                if (node.toLowest.depth > uNode.depth) {
                    node.toLowest = uNode;
                }
                if(uNode.depth<node.depth){
                    node.low.add(uId);
                }else{
                    node.low.remove(uId);
                }
            }
        }
        for(DfsNode chi:node.children){
            node.AllChiNum+=chi.AllChiNum;
        }
    }
    public void     updateVRemoval(int removedVId) {
        vIsOpen.set(removedVId,false);
        removedV = dfsNodes.get(removedVId);
        if (removedV == root) {
            updateRootRemoval();
            return;
        }
        removeChild(removedV.parent, removedV);
        lowestConnection = removedV.parent;
        for (Integer vId:removedV.low){
            if(!vIsOpen.get(vId))
                continue;
            if(dfsNodes.get(vId).depth<lowestConnection.depth)
                lowestConnection= dfsNodes.get(vId);
        }
        for (int i=0;i<removedV.children.size();++i) {
            node=removedV.children.get(i);
            pair = resetSubTreeAndFindBackConnection(node);
            backConnection = pair.Dfsnode_1;
            newTreeRoot = pair.Dfsnode_2;
            if(backConnection==null)
                System.out.println("s ");
            if (backConnection.depth < lowestConnection.depth) {
                lowestConnection = backConnection;
            }
            newTreeRoot.parent = backConnection;
            backConnection.children.add(newTreeRoot);
            newTreeRoot.depth = backConnection.depth + 1;
            buildDfsTreeRecur(newTreeRoot);
        }
        setUnrefreshed(removedV.parent, lowestConnection);
        repairDfsTreeToLowestRecur(lowestConnection);
        removedV.parent = null;
        removedV.children.clear();
        removedV.toLowest = null;
        removedV.refreshed = false;
        removedV.depth=-1;
        removedV.low.clear();
    }
    public void setUnrefreshed(DfsNode from, DfsNode toAncestor) {
        curr = from;
        while (curr != toAncestor) {
            curr.refreshed = false;
            curr = curr.parent;
        }
        toAncestor.refreshed = false;
        toAncestor.AllChiNum=1;
        curr=curr.parent;
        while(curr!=null){
            curr.AllChiNum--;
            curr=curr.parent;
        }
    }
    public Pair resetSubTreeAndFindBackConnection(DfsNode node) {
        nodeStack.clear();
        nodeStack.push(node);
        int highestDepth = -1;
        DfsNode newSubRoot = null, backConnection = null;
        final int barDepth = node.depth;
        while (!nodeStack.isEmpty()) {
            DfsNode curr = nodeStack.pop();
            for (DfsNode child : curr.children) {
                nodeStack.push(child);
            }
            for(Integer low:curr.low){
                if (!vIsOpen.get(low))
                    continue;
                lowNode=dfsNodes.get(low);
                if(lowNode.depth<barDepth&&lowNode.depth>highestDepth){
                    highestDepth = lowNode.depth;
                    backConnection = lowNode;
                    newSubRoot = curr;
                }
            }
            curr.depth = -1;
            curr.parent = null;
            curr.children.clear();
            curr.refreshed = false;
            Cut.remove(curr.vId);
            curr.low.clear();
        }
        return new Pair(backConnection, newSubRoot);
    }
    public void updateRootRemoval() {
        for (int i = 0; i < vIsOpen.size(); i++) {
            if (!vIsOpen.get(i)) continue;
            node = dfsNodes.get(i);
            node.depth -= 1;
            node.refreshed = false;
            node.AllChiNum=1;
        }
        oldRoot = root;
        root = root.children.get(0);
        root.parent = null;
        repairDfsTreeToLowestRecur(root);
        oldRoot.parent = null;
        oldRoot.children.clear();
        oldRoot.toLowest = null;
        oldRoot.refreshed = false;
        oldRoot.low.clear();
        oldRoot.depth=-1;
        oldRoot.AllChiNum=1;
    }
    public void repairDfsTreeToLowestRecur(DfsNode node) {
        Cut.remove(node.vId);
        node.toLowest = node;
        node.refreshed = true;
        node.AllChiNum=1;
        node.low.clear();
        for (DfsNode child : node.children) {
            if (!child.refreshed) {
                repairDfsTreeToLowestRecur(child);
            }
            if (child.toLowest.depth < node.toLowest.depth) {
                node.toLowest = child.toLowest;
            }
            if ((node == root && node.children.size() > 1) || (node != root && child.toLowest.depth >= node.depth)) {
                Cut.add(node.vId);
            }
        }
        for (int uId : graph.adjacentList.get(node.vId)) {
            if (!vIsOpen.get(uId)) continue;
            uNode = dfsNodes.get(uId);
            if (uNode.depth < node.toLowest.depth) {
                node.toLowest = uNode;
            }
            if(uNode.depth<node.depth){
                node.low.add(uId);
            }else{
                node.low.remove(uId);
            }
        }
        for(DfsNode chi:node.children){
            node.AllChiNum+=chi.AllChiNum;
        }
    }
    public void removeChild(DfsNode parent, DfsNode child) {
        children = parent.children;
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) != child) continue;
            if (i != children.size() - 1) {
                children.set(i, children.get(children.size() - 1));
            }
            children.remove(children.size() - 1);
        }
    }
    public void singleUpdate(int addedVId){
        DfsNode addV=dfsNodes.get(addedVId);
        root.children.add(addV);
        root.AllChiNum++;
        addV.parent=root;
        addV.children.clear();
        addV.depth= root.depth+1;
        addV.toLowest=addV;
        addV.refreshed=true;
        addV.AllChiNum=1;
        if(root.children.size()>1)
            Cut.add(root.vId);
        for(Integer integer:graph.adjacentList.get(addedVId)){
            addV.low.add(integer);
        }
    }
    public void updateVAddition(int addedVId) {
        vIsOpen.set(addedVId,true);
        if(graph.adjacentList.get(addedVId).size()==1&&graph.adjacentList.get(addedVId).get(0)==root.vId)
            singleUpdate(addedVId);
        /*vOnPath.addAll(Collections.nCopies(graph.vertices.size(), false));*/
        neighborsId = graph.adjacentList.get(addedVId);
        subRootId = neighborsId.get(0);
        for (int i = 1; i < neighborsId.size(); i++) {
            if(dfsNodes.get(neighborsId.get(i)).depth>dfsNodes.get(subRootId).depth&&vIsOpen.get(neighborsId.get(i)))
                subRootId=neighborsId.get(i);
        }
        if(dfsNodes.get(subRootId)==root){
            singleUpdate(addedVId);
            return;
        }
        subRoot = dfsNodes.get(subRootId);
        getVOnPathToRootSetUnrefreshed(subRoot);//refresh
        findConnectionsToPathAndSubSubRoot(neighborsId, subRootId);
        lowestConnectionNode.refreshed = false;
        for (int vId : connections) {
            curr = dfsNodes.get(vId).parent;
            while (curr != lowestConnectionNode) {
                curr.refreshed = false;
                curr = curr.parent;
            }
            vNode = dfsNodes.get(vId);
            removeChild(vNode.parent, vNode);
            resetSubTree(vNode);
        }
        addedNode = dfsNodes.get(addedVId);
        addedNode.depth = subRoot.depth + 1;
        addedNode.parent = subRoot;
        subRoot.children.add(addedNode);
        addedNode.children.clear();
        addedNode.refreshed = false;
        for (int vId : subSubRoots) {
            subSubR = dfsNodes.get(vId);
            addedNode.children.add(subSubR);
            subSubR.parent = addedNode;
            subSubR.depth = addedNode.depth + 1;
            buildDfsTreeRecur(subSubR);
        }
        repairDfsTreeToLowestRecur(lowestConnectionNode);
    }
        public void print(DynamicUpdate dynamicUpdate){
            List<DfsNode> dfsNode=  dynamicUpdate.dfsNodes;
            Queue<DfsNode> queue=new LinkedList<>();
            queue.add(root);
            while(!queue.isEmpty()){
                DfsNode curr=queue.poll();
                System.out.println("当前id:"+curr.vId+"\t");
                for (DfsNode dfsNode1:curr.children){
                    System.out.println("孩子id:"+dfsNode1.vId+"\t");
                queue.add(dfsNode1);
            }
            System.out.println("当前low为："+"\t");
            for (Integer dfsNode1:curr.low){
                System.out.println("lowid:"+dfsNode1+"\t");
            }
        }
    }
    public void resetSubTree(DfsNode node) {
        assert nodeStack.isEmpty();
        nodeStack.push(node);
        while (!nodeStack.isEmpty()) {
            DfsNode curr = nodeStack.pop();
            for (DfsNode child : curr.children) {
                nodeStack.push(child);
            }
            curr.depth = -1;
            curr.parent = null;
            curr.children.clear();
            curr.refreshed = false;
            Cut.remove(curr.vId);
            curr.AllChiNum=1;
        }
    }
    public void getVOnPathToRootSetUnrefreshed(DfsNode node) {
        vOnPathToRoot.clear();
        curr = node;
        while (curr != null) {
            vOnPathToRoot.add(curr.vId);
            curr.refreshed = false;
            curr.AllChiNum++;
            curr = curr.parent;
        }
    }
    public void findConnectionsToPathAndSubSubRoot(List<Integer> uNeighbors, int uId) {
        connections.clear();;
        subSubRoots.clear();
        lowestConnectionNode = dfsNodes.get(uId);
        for (int neighborId : uNeighbors) {
            if (!vIsOpen.get(neighborId) || neighborId == uId) continue;
            if (vOnPathToRoot.contains(neighborId)) {
                if (lowestConnectionNode == null || lowestConnectionNode.depth > dfsNodes.get(neighborId).depth) {
                    lowestConnectionNode = dfsNodes.get(neighborId);
                }
                continue;
            }
            currNode = dfsNodes.get(neighborId);
            if(currNode.parent==null)
                System.out.println("s ");
            while (!vOnPathToRoot.contains(currNode.parent.vId)) {
                currNode = currNode.parent;
            }
            if (currNode.parent.depth < lowestConnectionNode.depth) {
                lowestConnectionNode = currNode.parent;
            }
            currId = currNode.vId;
            if (!connections.contains(currId)) {
                connections.add(currId);
                subSubRoots.add(neighborId);
            }
        }
    }
}