import java.io.File;
import java.io.IOException;
import java.util.*;

public class AFD_TS {
    public static void main(String[] args) throws Exception {
        String filename=(args[0]);
        //String filename="D:/Lau/Lau/所有算例/LMS/v30_d30.dat";
        AFD_TS tt = new AFD_TS();
        tt.star(filename);
    }
    int bestK;
    int tabuTimes;
    int vertex_num;         //结点数
    int[][] Graph;          //结点矩阵
    int[] Tabu;             //禁忌数组
    int Thresh;
    int FocalD;
    int CurrentD;
    int Tabu_length;        //禁忌长度
    int Tabu_Iter;          //禁忌迭代次数
    int Iteration;          //运行次数
    int disturbIteration;   //扰动迭代次数
    int[] V_degree;         //节点度数矩阵
    int[] low;              //low[u]：表示顶点u及其子树中的点，通过反向边，能够回溯到的最早的点（dfn最小）的dfn值。
    int[] dfn;              //深度优先搜索，dfn[n] = x表示节点n在第x次被搜索到
    int deep;               //搜索深度
    int f;                 //评估函数
    int min_f;              //历史出现最小的冲突数
    int max_f;              //历史出现的最大冲突数
    int[] move;
    int[] ChooseNPlus;//存储动作数组
    int a;                  //评估函数系数,表示重要性
    int root;               //根节点
    InSet D_zpj;     //支配集
    InSet D_bzpj;    //被支配集
    InSet D_wgj;     //未被支配集
    InSet D_2cds;    //双联通支配集
    InSet D_articulation;    //割点集
    InSet NPlus;
    InSet NSub;
    InSet N;
    InSet DisTabu;
    InSet Sbest;
    Random r;
    String filename;        //文件名
    long startTime;         //获取开始时间
    long endTime;         //获取开始时间
    long moveTime;
    long moveEnd;
    /*DynamicUpdate dynamicUpdate;*/
    InSet TabuCuts;
    InSet Recover;
    int F;
    int ChiNum;
    OrderedList OrderedList;
    TabuDfsNode TabuRoot;
    ArrayList<TabuListNode> CheckTimes;
    int AvgF;
    int AvgTimes;
    DynamicUpdate dynamicUpdate;
    //读文件创建矩阵
    void Creat_Graph(String filename) throws IOException {
        Scanner scanner = new Scanner(new File(filename));
        //int BestSolution = scanner.nextInt();//记得改变算例第一行为此算例最佳解
        //bestK=BestSolution;
        int vNum = scanner.nextInt();
        int eNum = scanner.nextInt();
        Graph graph = new Graph(0);
        graph.creat(vNum);
        for (int i = 0; i < eNum; i++) {
            int source = scanner.nextInt();
            int target = scanner.nextInt();
            graph.G[graph.getIdOrAddV(source)][graph.getIdOrAddV(target)]=1;
            graph.G[graph.getIdOrAddV(target)][graph.getIdOrAddV(source)]=1;
        }
        Graph= graph.G;
        vertex_num=vNum;
        scanner.close();
    }

    void initialization(String file) throws Exception {
        filename = file;
        Creat_Graph(filename);
        dynamicUpdate=new DynamicUpdate(filename);
        dfn = new int[vertex_num ];
        low = new int[vertex_num ];
        Tabu = new int[vertex_num ];
        Tabu_Iter = 0;
        Iteration = 0;
        Tabu_length=1;
        move = new int[2];
        ChooseNPlus=new int[2];
        a = 70;
        min_f = 10000;
        max_f = 0;
        AvgTimes=0;
        V_degree = new int[vertex_num ];
        Sbest = new InSet(vertex_num );
        D_zpj = new InSet(vertex_num );
        D_bzpj = new InSet(vertex_num );
        D_wgj = new InSet(vertex_num );
        D_2cds = new InSet(vertex_num );
        D_articulation = new InSet(vertex_num );
        NPlus=new InSet(vertex_num);
        NSub=new InSet(vertex_num);
        N=new InSet(vertex_num);
        DisTabu=new InSet(vertex_num);
        Recover=new InSet(vertex_num);
        OrderedList=new OrderedList();
        r = new Random();
        TabuRoot=new TabuDfsNode(-1);
        deep = 0;
        CheckTimes=new ArrayList<>();
        init_D_zpj();
        getV_Degree();
    }

    //初始化支配集
    void init_D_zpj() {
        for (int i = 0; i < vertex_num ; i++) {
            D_zpj.add(i);
            OrderedList.add(i);
        }
    }

    //根据支配集初始化其他集合，并且初始化支配连通度
    void init_CDD(InSet Sbest) {
        Arrays.fill(V_degree, 0);
        for (Integer i : Sbest) {
            for (int j = 0; j < vertex_num ; j++) {
                if (Graph[i][j] != 0) {
                    if (Check_e_belong_D_set(j, Sbest)) {
                        V_degree[i]++;
                    } else {
                        V_degree[j]--;
                    }
                }
            }
        }
        D_bzpj.clear();
        D_wgj.clear();
        for (int j = 0; j < vertex_num ; j++) {
            if (V_degree[j] < 0) {
                D_bzpj.add(j);
            } else if (V_degree[j] == 0) {
                D_wgj.add(j);
            }
        }
    }
    //实验之前检查数据是否联通
    boolean Check_Data() {
        boolean b = true;
        for (int i = 0; i < vertex_num ; i++) {
            if (V_degree[i] == 0) {
                b = false;
                break;
            }
        }
        return b;
    }

    //获取每个节点度数
    void getV_Degree() {
        for (int i = 0; i < vertex_num; ++i) {
            for (int j = 0; j < vertex_num; ++j) {
                if (Graph[i][j] != 0) {
                    V_degree[i]++;
                }
            }
            //System.out.print(V_degree[i]+ " ");
        }
    }
    //选取节点最小的节点删除
    int find_MinDegree_Vertex() {
        int index = -1;
        int MinDegree = vertex_num +1;
        int same = 1;
        for (Integer i : D_zpj) {
            if (V_degree[i] < MinDegree && Tabu[i] <= Tabu_Iter) {
                index = i;
                MinDegree = V_degree[i];
                same = 0;
            } else if (V_degree[i] == MinDegree && Tabu[i] <= Tabu_Iter) {
                same++;
                if (r.nextInt(same) == 1) {
                    index = i;
                }
            }
        }
        return index;
    }
      /*int find_MinDegree_Vertex() {
        int index = -1;
        int MinDegree = vertex_num +1;
        int MinChiNum = vertex_num+1;
        int same = 1;
        List<DfsNode> dfsNodes=dynamicUpdate.dfsNodes;
        for (Integer i : D_zpj) {
            ChiNum=dfsNodes.get(i).AllChiNum;
            if (V_degree[i] < MinDegree && Tabu[i] <= Tabu_Iter) {
                index = i;
                MinDegree = V_degree[i];
                MinChiNum=ChiNum;
                same = 0;
            } else if (V_degree[i] == MinDegree && Tabu[i] <= Tabu_Iter) {
                if(ChiNum<MinChiNum){
                    index = i;
                    MinChiNum=ChiNum;
                    same=0;
                }else if(ChiNum==MinChiNum) {
                    same++;
                    if (r.nextInt(same) == 1) {
                        index = i;
                    }
                }
            }
        }
        return index;
    }*/
    //删除节点后，修改支配集节点度数,支配集中节点为正数，表示其相邻的点为支配集的个数
    //被支配集中的节点为负数，负号为标志 ， 数值表示其与支配集中节点连接的个数；
    //未被支配的节点为0；
    //删除动作
    void Delete(int e) {
        D_zpj.remove(e);
        OrderedList.remove(e);
        V_degree[e] = 0;
        for (int i = 0; i < vertex_num; i++) {
            if (Graph[i][e] == 1) {
                if (Check_e_belong_D_set(i, D_zpj)) {
                    V_degree[i]--;
                    V_degree[e]--;
                } else {
                    V_degree[i]++;
                    if (V_degree[i] == 0) {
                        D_bzpj.remove(i);
                        D_wgj.add(i);
                    }
                }
            }
        }
        //最后依据符号判定e节点以及其领域属于什么集合
        if (V_degree[e] == 0) {
            D_wgj.add(e);
        } else if (V_degree[e] < 0) {
            D_bzpj.add(e);
        }
        /*System.out.println("delete:" + e);*/

    }
    //增加动作
    void Add(int e) {
        D_zpj.add(e);
        OrderedList.add(e);
        D_bzpj.remove(e);
        V_degree[e] = 0;
        for (int i = 0; i < vertex_num; i++) {
            if (Graph[i][e] == 1) {
                if (Check_e_belong_D_set(i, D_zpj)) {
                    V_degree[i]++;
                    V_degree[e]++;
                } else {
                    V_degree[i]--;
                    if (Check_e_belong_D_set(i, D_wgj)) {
                        D_wgj.remove(i);
                        D_bzpj.add(i);
                    }
                }
            }
        }
    }
    void find_move2() {
        InSet D_zpjplus = new InSet(D_zpj);
        InSet D_bzpjplus = new InSet(D_bzpj);   //应该为bzpj+wgj
        int index_f;
        int Min_f = 100000;
        int same = 1;
        move[0]=-1;
        move[1]=-1;
        for (Integer i : dynamicUpdate.Cut) {
            D_zpjplus.remove(i);
        }
        for (Integer i : D_zpjplus) {
            Delete(i);
            dynamicUpdate.updateVRemoval(i);
            index_f = a * D_wgj.size() + (100 - a) * dynamicUpdate.Cut.size();
            if (index_f == 0) {
                move[0] = i;
                move[1] = -1;
                //System.out.println("直接减少不添加");
                /*D_2cds.clear();
                D_2cds.copy_from(D_zpj);*/
                break;
            }
            for (Integer j : D_bzpjplus) {
                if (V_degree[j] == 0||Tabu[j]>Tabu_Iter ) {
                    ////删除节点i之后节点J的度数为零，则表示节点J仅与支配集中的节点I相连
                } else {
                    Add(j);
                    dynamicUpdate.updateVAddition(j);
                    index_f = a * D_wgj.size() + (100 - a) * dynamicUpdate.Cut.size();
                    if (index_f < Min_f||index_f==0) {
                        move[0] = i;
                        move[1] = j;
                        same = 1;
                        Min_f = index_f;
                    } else if (index_f == Min_f) {
                        same++;
                        if (r.nextInt(same) == 1) {
                            move[0] = i;
                            move[1] = j;
                        }
                    }
                    Delete(j);
                    dynamicUpdate.updateVRemoval(j);
                }
            }
            Add(i);
            dynamicUpdate.updateVAddition(i);
        }
    }
    void single_move() {
        InSet D_zpjplus = new InSet(D_zpj);
        InSet D_bzpjplus = new InSet(D_bzpj);   //应该为bzpj+wgj
        int index_f;
        int Min_f = 100000;
        int Min_v = vertex_num+1;
        int same = 1;
        move[0]=-1;
        move[1]=-1;
        for (Integer i : dynamicUpdate.Cut) {
            D_zpjplus.remove(i);
        }
        for (Integer i : D_zpjplus) {
            Delete(i);
            index_f = a * D_wgj.size();
            if(index_f==0){
                move[0] = i;
                move[1] =-1;
                return;
            }
            for (Integer j : D_bzpjplus) {
                if (V_degree[j] == 0||Tabu[i] > Tabu_Iter) {
                    ////删除节点i之后节点J的度数为零，则表示节点J仅与支配集中的节点I相连
                } else {
                    Add(j);
                    index_f = a * D_wgj.size() + (100 - a) * dynamicUpdate.Cut.size();
                    if (index_f < Min_f&&V_degree[i]<Min_v) {
                        move[0] = i;
                        move[1] = j;
                        same = 1;
                        Min_f = index_f;
                    } else if (index_f == Min_f&&V_degree[i]==Min_v) {
                        same++;
                        if (r.nextInt(same) == 1) {
                            move[0] = i;
                            move[1] = j;
                        }
                    }
                    Delete(j);
                }
            }
            Add(i);
        }
    }
    //执行交换动作
    void  make_move() {
        find_move2();
        if (move[1] == -1) {
            if(move[0]==-1){
                Tabu_length=1;
                return;
            }
            /*Delete(move[0]);*/
            /*dynamicUpdate.updateVRemoval(move[0]);*/
            Tabu[move[0]] = Tabu_Iter + Tabu_length;
            Tabu_Iter++;
        } else {
            Delete(move[0]);
            dynamicUpdate.updateVRemoval(move[0]);
            Tabu[move[0]] = Tabu_Iter +Tabu_length;
            Add(move[1]);
            dynamicUpdate.updateVAddition(move[1]);
            Tabu_Iter++;
        }
    }
    void  make_move2() {
        single_move();
        if (move[1] == -1) {
            if(move[0]!=-1){
                dynamicUpdate.updateVRemoval(move[0]);
                Tabu[move[0]]=Tabu_Iter+vertex_num/10;
                Tabu_Iter++;
            }
        } else {
            Delete(move[0]);
            dynamicUpdate.updateVRemoval(move[0]);
            Add(move[1]);
            dynamicUpdate.updateVAddition(move[1]);
            Tabu[move[0]]=Tabu_Iter+vertex_num/10;
            Tabu_Iter++;
        }
    }
    //检验节点属于什么集合,true 为属于
    boolean Check_e_belong_D_set(int e, InSet D) {
        boolean b = false;
        for (Integer i : D) {
            if (e == i) {
                b = true;
                break;
            }
        }
        return b;
    }


    //使用之前初始化DFN,LOW,D_articulation,deep,并从D_zpj中随机选择一个节点当做root
    int init_tarjan(InSet D_zpj) {
        for (int i = 0; i < vertex_num ; i++) {
            dfn[i] = 0;
            low[i] = 0;
        }
        deep = 0;
        D_articulation.clear();
        int[] D_zpj_array = D_zpj.to_array();
        root = D_zpj_array[0];
        return root;
    }

    //Tarjan算法
    void tar_jan(int v, int father, InSet D_zpj) {
        dfn[v] = low[v] = ++deep;
        int child = 0;
        for (Integer i : D_zpj) {
            if (Graph[v][i] == 1 && v != i) {
                if (dfn[i] == 0) {           //节点v未被访问，则(u,v)为树边
                    child++;
                    tar_jan(i, v, D_zpj);
                    low[v] = Math.min(low[v], low[i]);
                    if (v != root && low[i] >= dfn[v]) {     //不为根结点但是满足第二类条件的节点
                        D_articulation.add(v);
                    }
                    if (v == root && child >= 2) {   //  如果当前节点是根节点并且儿子个数大于等于2，则满足第一类节点，为割点
                        D_articulation.add(v);
                    }
                } else if (i != father) {   // //节点v已访问，则(u,v)为回边
                    low[v] = Math.min(low[v], dfn[i]);
                }
            }
        }
    }

    void setF() {
        if (max_f < f) {
            max_f = f;
        }
        if (min_f > f) {
            min_f = f;
        }
        AvgF=(min_f+max_f)/4;
        if(f>AvgF){
            AvgTimes++;
        }else{
            AvgTimes=0;
        }
        if(AvgTimes>400){
            //System.out.println("track back");
            for(int i=0;i<vertex_num;i++){
                Tabu[i]=Tabu_Iter;
            }
            FindLessSet();
            AvgTimes=0;
            Tabu_length=1;
        }
    }
    void FocalDistance(){
        //System.out.println("raodong 0 ");
        Phase0();
        //System.out.println("raodong 1");
        Phase1();
        //System.out.println("raodong 2");
        Phase3();
    }
    public void InitPhase0(){
        FindLessSet();
        Thresh= (int) ( f*1.75);
        if(D_zpj.size()-dynamicUpdate.Cut.size()>D_bzpj.size()){
            FocalD=(int)((D_bzpj.size())*0.7);
        }else{
            FocalD=(int)((D_zpj.size()-dynamicUpdate.Cut.size())*0.7);
        }
        CurrentD=0;
        N.copy_from(D_zpj);
        NPlus.copy_from(D_zpj);
        NSub.clear();
        DisTabu.clear();
    }
    public void FindLessSet(){
        int minTimes=1000000;
          int same=0;
          InSet BestD=new InSet(vertex_num);
          TabuDfsNode bestSet=new TabuDfsNode();
          for(TabuListNode tabuListNode:CheckTimes){
              if(tabuListNode.times<minTimes){
                  bestSet=tabuListNode.par;
                  minTimes=tabuListNode.times;
                  same=1;
              }else if(tabuListNode.times==minTimes){
                  same++;
                  if(r.nextInt(same)==1){
                      bestSet=tabuListNode.par;
                  }
              }
          }
          OrderedList.clear();
          Collections.fill(dynamicUpdate.vIsOpen,false);
          while(bestSet.id!=-1){
              BestD.add(bestSet.id);
              OrderedList.add(bestSet.id);
              dynamicUpdate.vIsOpen.set(bestSet.id,true);
              bestSet=bestSet.par;
          }
          D_zpj=BestD;
          init_CDD(D_zpj);
          dynamicUpdate.findAllCuts();
          f = a * D_wgj.size() + (100 - a) * dynamicUpdate.Cut.size();
    }
/*    public void FindBestSet(){
        int minF=100000;
        int same=0;
        InSet BestD=new InSet(vertex_num);
        TabuDfsNode bestSet=new TabuDfsNode();
        for(TabuListNode tabuListNode:CheckTimes){
            if(tabuListNode.f<minF){
                bestSet=tabuListNode.par;
                minF=tabuListNode.f;
                same=1;
            }else if(tabuListNode.f==minF){
                same++;
                if(r.nextInt(same)==1){
                    bestSet=tabuListNode.par;
                }
            }
        }
        OrderedList.clear();
        Collections.fill(dynamicUpdate.vIsOpen,false);
        while(bestSet.id!=-1){
            BestD.add(bestSet.id);
            OrderedList.add(bestSet.id);
            dynamicUpdate.vIsOpen.set(bestSet.id,true);
            bestSet=bestSet.par;
        }
        D_zpj=BestD;
        init_CDD(D_zpj);
        dynamicUpdate.findAllCuts();
    }*/
    public void Phase0(){
        InitPhase0();
        boolean flag=false;
        while(CurrentD<FocalD||f<Thresh){
            while(!flag){
            for(Integer integer:D_zpj){
                TabuCuts=dynamicUpdate.Cut;
                if(TabuCuts.contains(integer)||DisTabu.contains(integer)||r.nextInt(2)!=1) {
                    continue;
                }
                else {
                    Delete(integer);
                    dynamicUpdate.updateVRemoval(integer);
                    DisTabu.add(integer);
                    NPlus.remove(integer);
                    flag=true;
                    break;
                }
            }
        }
        while (flag){
            for(Integer integer1:D_bzpj){
                if(DisTabu.contains(integer1)||r.nextInt(2)!=1) {
                    continue;
                }
                else {
                    Add(integer1);
                    dynamicUpdate.updateVAddition(integer1);
                    /*DisTabu.add(integer1);*/
                    NSub.add(integer1);
                    flag=false;
                    CurrentD++;
                    break;
                }
            }
        }
            /*tar_jan(init_tarjan(D_zpj), -1, D_zpj);*/
            f= a * D_wgj.size() + (100 - a) * dynamicUpdate.Cut.size();
        }
        FocalD=CurrentD;
    }
/*        for(Integer integer:D_zpj){
            if(f>=Thresh)
                break;
            TabuCuts=dynamicUpdate.Cut;
            if(TabuCuts.contains(integer)||DisTabu.contains(integer)||r.nextInt(2)!=1) {
                continue;
            }
            else {
                Delete(integer);
                dynamicUpdate.updateVRemoval(integer);
                DisTabu.add(integer);
                NPlus.remove(integer);
            }
            for(Integer integer1:D_bzpj){
                if(DisTabu.contains(integer1)||r.nextInt(2)!=1) {
                    continue;
                }
                else {
                    Add(integer1);
                    dynamicUpdate.updateVAddition(integer1);
                    DisTabu.add(integer1);
                    NSub.add(integer1);
                    break;
                }
            }
            CurrentD++;
            f=a*D_wgj.size()+(100-a)*dynamicUpdate.Cut.size();
        }*/
    public void FocalDistanceMoveFind(){
        InSet D_zpjplus = new InSet(D_zpj);
        InSet D_bzpjplus = new InSet(D_bzpj);
        int index_f;
        int Min_f = f;
        int same = 1;
        move[0]=-1;
        move[1]=-1;
        ChooseNPlus[0]=-1;
        ChooseNPlus[1]=-1;
        for (Integer i : dynamicUpdate.Cut) {
            D_zpjplus.remove(i);
        }
        for (Integer i : D_zpjplus) {
            if(!N.contains(i))
                continue;
            Delete(i);
            dynamicUpdate.updateVRemoval(i);
            /*tar_jan(init_tarjan(D_zpj), -1, D_zpj);*/
            index_f = a * D_wgj.size() + (100 - a) * dynamicUpdate.Cut.size();
            if (index_f == 0) {
                move[0] = i;
                move[1] = -1;
                break;
            }
            for (Integer j : D_bzpjplus) {
                if (V_degree[j] == 0) {
                    ////删除节点i之后节点J的度数为零，则表示节点J仅与支配集中的节点I相连
                } else {
                    Add(j);
                    dynamicUpdate.updateVAddition(j);
                    /*tar_jan(init_tarjan(D_zpj), -1, D_zpj);*/
                    index_f = a * D_wgj.size() + (100 - a) * dynamicUpdate.Cut.size();
                    if (index_f < Min_f) {
                        move[0] = i;
                        move[1] = j;
                        same = 1;
                        Min_f = index_f;
                        if(NPlus.contains(i)){
                            ChooseNPlus[0]=i;
                            ChooseNPlus[1]=j;
                        }
                    } else if (index_f == Min_f&&index_f<f) {
                        same++;
                        if (r.nextInt(same) == 1) {
                            move[0] = i;
                            move[1] = j;
                        }
                    }
                    Delete(j);
                    dynamicUpdate.updateVRemoval(j);
                }
            }
            Add(i);
            dynamicUpdate.updateVAddition(i);
        }
    }

    public void Phase1(){
        boolean stop=false;
        Recover.clear();
        long ii=System.currentTimeMillis();
        while(!stop){
            long iii=System.currentTimeMillis();
            if(iii-ii>5000){
                System.out.println("overtime");
                return;
            }
            /*tar_jan(init_tarjan(D_zpj), -1, D_zpj);*/
            f=a*D_wgj.size()+(100-a)*dynamicUpdate.Cut.size();
            FocalDistanceMoveFind();
            if(move[1]==-1){
                if(move[0]==-1){
                    return;
                }else{
                    Delete(move[0]);
                }
            }else{
                if(NPlus.contains(move[0])||CurrentD>FocalD){
                    Delete(move[0]);
                    dynamicUpdate.updateVRemoval(move[0]);
                    Add(move[1]);
                    dynamicUpdate.updateVAddition(move[1]);
                    DisTabu.add(move[0]);
                    if(NPlus.contains(move[0])){
                        NPlus.remove(move[0]);
                        NSub.add(move[1]);
                        CurrentD++;
                    }else{
                        NSub.remove(move[0]);
                        NSub.add(move[1]);
                    }
                } else if (CurrentD==FocalD&&NSub.contains(move[0])) {
                    Recover=new InSet(D_zpj);
                    Delete(ChooseNPlus[0]);
                    dynamicUpdate.updateVRemoval(ChooseNPlus[0]);
                    Add(ChooseNPlus[1]);
                    dynamicUpdate.updateVAddition(ChooseNPlus[1]);
                    /*tar_jan(init_tarjan(D_zpj), -1, D_zpj);*/
                    F = a * D_wgj.size() + (100 - a) * dynamicUpdate.Cut.size();
                    if(F<f){
                        DisTabu.add(ChooseNPlus[0]);
                        NPlus.remove(ChooseNPlus[0]);
                        NSub.add(ChooseNPlus[1]);
                        CurrentD++;
                    }else{
                        DisTabu.add(ChooseNPlus[0]);
                        NPlus.remove(ChooseNPlus[0]);
                        NSub.add(ChooseNPlus[1]);
                        CurrentD++;
                        FocalDistanceMoveFind();
                        if(move[1]==-1){
                            if(move[0]==-1){
                                D_zpj=Recover;
                                init_CDD(D_zpj);
                                OrderedList.clear();
                                Collections.fill(dynamicUpdate.vIsOpen,false);
                                for(Integer integer:D_zpj){
                                    OrderedList.add(integer);
                                    dynamicUpdate.vIsOpen.set(integer,true);
                                }
                                dynamicUpdate.findAllCuts();
                                return;
                            }
                        }else{
                            DisTabu.add(ChooseNPlus[0]);
                            NPlus.remove(ChooseNPlus[0]);
                            NSub.add(ChooseNPlus[1]);
                            CurrentD++;
                        }
                    }
                }
            }
        }
    }
    public void Phase3(){
        for(int i=0;i<vertex_num;i++){
            Tabu[i]=Tabu_Iter;
        }
        /*Tabu_length=1;*/
        Tabu_length=D_bzpj.size()/5;
        /*TabuRoot.chi.clear();*/
        /*CheckTimes.clear();*/
        /*min_f=10000;
        max_f=0;*/
    }
    //扰动
    /*void disturb() {
        disturbIteration = 0;
        int[] disturbAraay = new int[vertex_num + 1];
        int length = D_zpj.size() / 3;
        init_CDD(Sbest);
        for (int i = 0; i < length; i++) {
            InSet D_zpjplus = new InSet(D_zpj);
            InSet D_bzpjplus = new InSet(D_bzpj);
            tar_jan(init_tarjan(D_zpj), -1, D_zpj);
            for (Integer j : D_articulation) {
                D_zpjplus.remove(j);
            }
            for (int k = 1; k < disturbAraay.length; k++) {
                if (disturbAraay[k] == 1) {
                    D_zpjplus.remove(k);
                }
            }
            if (D_zpjplus.size() != 0) {
                int index = 0;
                int x = 0;
                int randomNumber = r.nextInt(D_zpjplus.size() + 1);
                for (Integer k : D_zpjplus) {
                    index++;
                    if (index == randomNumber) {
                        x = k;
                    }
                }
                Delete(x);
                disturbAraay[x] = 1;
                for (Integer k : D_bzpjplus) {
                    if (disturbAraay[k] == 1 || V_degree[k] == 0) {
                        D_bzpjplus.remove(k);
                    }
                }
                if (D_bzpjplus.isEmpty()) {
                    Add(x);
                    disturbAraay[x] = 0;
                } else {
                    for (Integer k : D_bzpjplus) {
                        if(Check_e_belong_D_set(k,D_bzpj)){
                            disturbAraay[k] = 1;
                            Add(k);
                            break;
                        }
                    }

                }
            }
        }
        D_wgj.remove(0);
    }*/

    //-----------------------------开始实验----------------------------------
    void star(String filename) throws Exception {
        float totalK=0,totalTime=0;
        int run;
        for( run=0;run<1;run++){
            initialization(filename);
            int k=vertex_num;
            tabuTimes=0;
            if (!Check_Data()) {
                System.out.println("数据集未联通，停止实验！");
            } else {
                startTime = System.currentTimeMillis(); //获取开始时间
                System.out.println("-----------------------开始实验---------------------------------");
                k = D_zpj.size();
                while (Iteration < 10000 && D_zpj.size() > 2) {
                    if (D_zpj.size() > (k - 1)) {
                        int v = find_MinDegree_Vertex();
                        Delete(v);//删除V 同时更新各集合以及各顶点度数
                        /*tar_jan(init_tarjan(D_zpj), -1, D_zpj);*/
                        dynamicUpdate.updateVRemoval(v);
                        if(dynamicUpdate.Cut.isEmpty()&&D_wgj.size()==0)
                             f=0;
                        else if(dynamicUpdate.Cut.isEmpty()){
                            a=100;
                            f=a*D_wgj.size();
                        }else{
                            a=100;
                            f=70*D_wgj.size()+30*dynamicUpdate.Cut.size();
                        }
                    } else {
                        if (f > 0&&a!=100) {
                            make_move();
                            /*tar_jan(init_tarjan(D_zpj), -1, D_zpj);*/
                            /*if(!D_articulation.equals(dynamicUpdate.Cut)){
                                System.exit(9);
                            }*/
                            f = a * D_wgj.size() + (100 - a) * dynamicUpdate.Cut.size();
                            Iteration++;
                            //System.out.println("IT:"+Iteration);
                            /*if (Iteration==4999){
                                System.out.println("aaa");
                            }*/
                            if(f<min_f){
                                TabuRoot.chi.clear();
                                CheckTimes.clear();
                                CheckOrCreatTabuTree();
                                min_f=f;
                            }else if(f==min_f){
                                CheckOrCreatTabuTree();
                            }else setF();
                            if(Iteration==5000){
                                FindLessSet();
                                TabuRoot.chi.clear();
                                CheckTimes.clear();
                                min_f = 100000;
                                max_f = 0;
                                Tabu_length=1;
                                for(int i=0;i<vertex_num;i++){
                                    Tabu[i]=Tabu_Iter;
                                }
                                a=50;
                            }
                            /*System.out.println("F:" + f+"size:"+CheckTimes.size()+"AVGtimes:"+AvgTimes);*/
                        }else if(f > 0&&a==100) {
                            make_move2();
                            if(!dynamicUpdate.Cut.isEmpty()) {
                                a = 70;
                                /*System.out.println("解决不了");*/
                            }
                            f = a * D_wgj.size() + (100 - a) * dynamicUpdate.Cut.size();
                            Iteration++;
                        }else if(f==0){
                            k = D_zpj.size();
                            Iteration = 0;
                            min_f = 100000;
                            max_f = 0;
                            AvgTimes=0;
                            TabuRoot.chi.clear();
                            CheckTimes.clear();
                            Tabu_length=1;
                            /*for(int i=0;i<vertex_num;i++){
                                Tabu[i]=Tabu_Iter;
                            }*/
                            /*Sbest.clear();*/
                            /*D_2cds.clear();
                            D_2cds.copy_from(D_zpj);*/
                            System.out.println("D_2cds number is " + D_zpj.size());
                            System.out.println("D_2cds number is " + "：" + check_D_2cds_plus(D_zpj) + ";" + D_zpj);
                            endTime = System.currentTimeMillis(); //获取结束时间
                            /*System.out.println("k:"+k+"共用时：" + (endTime - startTime) + "ms");*/ //输出运行时间
                            /*if(!check_D_2cds_plus(D_zpj)){
                                System.exit(111);
                            }*/
                            if(k==bestK){
                                System.out.println("check solution");
                                /*totalTime+=endTime-startTime;
                                System.out.println("total：" + totalTime);*/
                                if(!check_D_2cds_plus(D_zpj)){
                                    System.exit(111);
                                }
                                if(!check_D_2cds(D_zpj)){
                                    System.exit(555);
                                }
                                break;
                            }
                        }
                    }
                }
                totalK+=k;
                totalTime+=(endTime - startTime);
            }
            System.out.println("totalK:" + totalK);
            System.out.println("totalTime:" + totalTime);
            System.out.println("run:" + (run+1));
            System.out.println("avgK:" + totalK / (run+1));
            System.out.println("avgTime:" + totalTime / (run+1));
        }
    }
    void CheckOrCreatTabuTree(){
        TabuDfsNode curr=TabuRoot;
        boolean flag;
        Node znode=OrderedList.firstNode;
        for(int j=0;j<OrderedList.length;j++){
            flag=false;
            int integer= (int) znode.getData();
            for(int i=0;i<curr.chi.size();++i){
                if(curr.chi.get(i).id==integer){
                    curr=curr.chi.get(i);
                    flag=true;
                    break;
                }
            }
            if(!flag){
                TabuDfsNode node=new TabuDfsNode(integer);
                curr.chi.add(node);
                node.par=curr;
                curr=node;
            }
            znode=znode.getLink();
        }
        if(curr.times==null){
            CheckTimes.add(new TabuListNode());
            curr.times=CheckTimes.get(CheckTimes.size()-1);
            CheckTimes.get(CheckTimes.size()-1).par=curr;
        }
        curr.times.times++;
        curr.times.f=f;
        if (curr.times.times>3){
            /*System.out.println("扰动---------------------------------------------------------------------------");
            System.out.println("扰动前支配集大小为：" + D_zpj.size() + "; " + D_zpj);
            System.out.println("扰动前被支配集大小为：" + D_bzpj.size() + "; " + D_bzpj);
            System.out.println("扰动前无关集大小为：" + D_wgj.size() + "; " + D_wgj);*/
            //System.out.println("扰动");
            FocalDistance();
            /*System.out.println("扰动后支配集大小为：" + D_zpj.size() + "; " + D_zpj);
            System.out.println("扰动后被支配集大小为：" + D_bzpj.size() + "; " + D_bzpj);
            System.out.println("扰动后无关集大小为：" + D_wgj.size() + "; " + D_wgj);*/
            //System.out.println("扰动结束");
            /*min_f = f;*/
            return;
        }
        if(curr.times.times>1)
            Tabu_length+=vertex_num/30;
            //Tabu_length+=1;
    }
    //用来记录局部最优格局
    //结束后再次检验双联通支配集是否符合
    boolean check_D_2cds(InSet D_2cds) {
        HashSet<Integer> a_plus = new HashSet<>();
        int[] integers = D_2cds.to_array();
        for (int i = 0; i < vertex_num ; i++) {
            a_plus.add(i);
        }
        for (Integer integer : integers) {
            a_plus.remove(integer);
            for (int j = 0; j < vertex_num; j++) {
                if (Graph[integer][j] != 0) {
                    a_plus.remove(j);
                }
            }
        }
        boolean b = a_plus.isEmpty();
        if(!b){
            System.out.println("不是支配集");
            return false;
        }
        if (!is2EdgeConnected(D_2cds)) {
            System.out.println("不是双连通");
            return false;
        }
        return true;
    }
    private boolean is2EdgeConnected(InSet D_2cds) {
        for (Integer removeNode : D_2cds) {
            InSet remainingNodes = new InSet(D_2cds);
            remainingNodes.remove(removeNode);
            if (!isConnected(remainingNodes)) {
                return false;
            }
        }
        return true;
    }

    private boolean isConnected(InSet nodes) {
        if (nodes.isEmpty()) {
            return true;
        }
        Integer start = nodes.iterator().next();
        Stack<Integer> stack = new Stack<>();
        InSet visited = new InSet(vertex_num);
        stack.push(start);
        while (!stack.isEmpty()) {
            Integer node = stack.pop();
            if (!visited.contains(node)) {
                visited.add(node);
                for (int j = 0; j < vertex_num; j++) {
                    if (Graph[node][j] != 0 && nodes.contains(j)) {
                        stack.push(j);
                    }
                }
            }
        }
        return visited.equals(nodes);
    }
    boolean check_D_2cds_plus(InSet D_2cds) {
        HashSet<Integer> a_plus = new HashSet<>();
        int[] integers = D_2cds.to_array();
        for (int i = 0; i < vertex_num ; i++) {
            a_plus.add(i);
        }
        for (Integer integer : integers) {
            a_plus.remove(integer);
            for (int j = 0; j < vertex_num; j++) {
                if (Graph[integer][j] != 0) {
                    a_plus.remove(j);
                }
            }
        }
        boolean b = a_plus.isEmpty();
        int index = 0;
        for (Integer i : D_2cds) {
            //System.out.print(i + "->");
            for (Integer j : D_2cds) {
                if (Graph[i][j] == 1) {
                    index++;
                }
            }
            if (index < 2) {
                b = false;
            }
            index = 0;
        }
        return b;
    }
}


