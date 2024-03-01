package testing.MyJunitTest;

import java.util.LinkedList;
import java.util.function.Function;

/**
 * @Author 3590
 * @Date 2024/3/1 20:06
 * @Description 使用邻接表表示图
 * 连通图的深度优先遍历
 * 从图中某个顶点V0 出发，访问此顶点，然后依次从V0的各个未被访问的邻接点出发深度优先搜索遍历图，直至图中所有和V0有路径相通的顶点都被访问到
 */
public class MyGraph<T> {
    private int nodeNum;
    T[] nodes; // it will store nodes values (commitId)
    LinkedList<Integer>[] adj;
    public MyGraph(int nodeNum) {
        this.nodeNum = nodeNum;
        this.adj = new LinkedList[nodeNum]; // an array, where all type is LinkedList
        this.nodes = (T[]) new Object[nodeNum];
        for (int i = 0; i < this.nodeNum; ++i) {
            adj[i] = new LinkedList<>();
        }
    }

    public void setNodesVal(T[] valArray) {
        System.arraycopy(valArray, 0, nodes, 0, nodes.length);
    }

    /**
     * create a path: v --> w
     */
    public void addEdge(int v, int w) {
        adj[v].add(w);
    }

    public void DFS(int startIdx, boolean[] visited) {
        visited[startIdx] = true;
        System.out.println(nodes[startIdx]);
        // 遍历访问其所有未访问过的邻居
        for (int adjIdx : adj[startIdx]) {
            if (!visited[adjIdx]) {
                DFS(adjIdx, visited);
            }
        }
    }

    public void traverseGraph() {
        boolean[] visited = new boolean[this.nodeNum];
        for (int i = 0; i < visited.length; ++i) {
            if (!visited[i]) {
                DFS(i, visited);
            }
        }
    }

}
