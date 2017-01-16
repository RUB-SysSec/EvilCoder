package tools.data_flow;
import com.tinkerpop.gremlin.java.*;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Vertex;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Edge;
import com.tinkerpop.blueprints.Vertex;

import com.tinkerpop.pipes.PipeFunction;

import org.neo4j.graphdb.Node;

import java.util.ArrayList;
import java.util.List;

public class Pipeline extends GremlinPipeline
 {
 public static Neo4j2Graph g;

// Starter-Methods
  public static Pipeline v(Long node_id)
   {
    return new Pipeline().start(g.getVertex(node_id));
   }

  public static Pipeline e(Long edge_id)
   {
    return new Pipeline().start(g.getEdge(edge_id));
   }

  public static Pipeline Vs()
   {
    return new Pipeline().start_Neo4j(g.getVertices());
//   GremlinPipeline pipe = new GremlinPipeline().start(g.getVertices());
//   return (Pipeline)pipe;
   }

  public Pipeline start_Neo4j(Iterable<Vertex> vs)
   {
    return (Pipeline)(super.start(vs));
   }

  public static Pipeline v(List<Long> node_ids)
   {
   List<Node> nodes = new ArrayList<>();
     for(Long id : node_ids)
      {
       nodes.add(g.getVertex(id).getRawVertex());
      }
    return new Pipeline().start(nodes);
   }



// Helper
  public List<Node> to_list()
   {
   List<Node> ret = new ArrayList<>();
   List<Neo4j2Vertex> vertices = super.toList();
     for(Neo4j2Vertex v : vertices)
      {
       ret.add(v.getRawVertex());
      }

    return ret;
   }


// JoernSteps

  public Pipeline parents()
   {
    return (Pipeline)this.in("IS_AST_PARENT");
   }

  public Pipeline children()
   {
    return (Pipeline)this.out("IS_AST_PARENT");
   }

  public Pipeline defines()
   {
    return (Pipeline)this.out("DEF");
   }


//  public Pipeline functionToStatements()
//   {
//    return (Pipeline)(this.transform(
//             new PipeFunction<Neo4j2Vertex,List<Node>>()
//              {
//               public List<Node> compute(Neo4j2Vertex it)
//                {
//                 return Joern_db.queryNodeIndex("isCFGNode:True AND functionId:" + it.getId().toString());
//                }
//              }
//           ).scatter());
//   }

  public Pipeline functionToStatements()
   {
    return (Pipeline)(this.transform(
             new PipeFunction<Neo4j2Vertex,List<Neo4j2Vertex>>()
              {
               public List<Neo4j2Vertex> compute(Neo4j2Vertex it)
                {
                List<Node> tmp = Joern_db.queryNodeIndex("isCFGNode:True AND functionId:" + it.getId().toString());
                List<Neo4j2Vertex> ret = new ArrayList<>();
                  for(Node n : tmp)
                   {
                    ret.add(new Neo4j2Vertex(n, Joern_db.g) );
                   }
                 return ret;
                }
              }
           ).scatter());
   }


//  public Pipeline has(final String property, final Object value)
//   {
//return (Pipeline)(this.filter(
//new PipeFunction<Node,Boolean>()
// {
//  public Boolean compute(Node it) {return it.getProperty(property).equals(value);}
// }
//));
//   }


  public Pipeline functionToAST()
   {
    return (Pipeline)(this.out("IS_FUNCTION_OF_AST"));
   }

  public Pipeline callToCallee()
   {
    return ((Pipeline)(this.out("IS_AST_PARENT"))).has("type", "Callee");
   }

  public Pipeline calleeToCall()
   {
    return (Pipeline)(this.in("IS_AST_PARENT"));
   }

  public Pipeline callToArguments()
   {
    return (Pipeline)(this.children().has("type", "ArgumentList").children());
   }

  public Pipeline lval()
   {
    return ((Pipeline)(this.out("IS_AST_PARENT"))).has("childNum", "0");
   }

  public Pipeline rval()
   {
    return ((Pipeline)(this.out("IS_AST_PARENT"))).has("childNum", "1");
   }

  public Pipeline uses()
   {
    return (Pipeline)(this.out("USE"));
   }

  public Pipeline ithArguments(Long i)
   {
    return ((Pipeline)(this.callToArguments())).has("childNum", i.toString());
   }





 // Overwriting original GremlinPipeline-Functions to return Pipeline
  public Pipeline start(Neo4j2Vertex object)
   {
    return (Pipeline)(super.start(object));
   }

  public Pipeline start(Neo4j2Edge object)
   {
    return (Pipeline)(super.start(object));
   }

  public Pipeline start(List<Node> nodes)
   {
   ArrayList<Neo4j2Vertex> vertices = new ArrayList<Neo4j2Vertex>();
     for(Node n : nodes)
      {
       vertices.add(new Neo4j2Vertex(n, g));
      }
    return (Pipeline)(super.start(vertices));
   }

  public Pipeline in(String... labels)
   {
    return (Pipeline)(super.in(labels));
   }

  public Pipeline out(String... labels)
   {
    return (Pipeline)(super.out(labels));
   }

  public Pipeline inE(String... labels)
   {
    return (Pipeline)(super.inE(labels));
   }

  public Pipeline outE(String... labels)
   {
    return (Pipeline)(super.outE(labels));
   }

  public Pipeline property(String prop)
   {
    return (Pipeline)(super.property(prop));
   }

  public Pipeline outV()
   {
    return (Pipeline)(super.outV());
   }

  public Pipeline has(String key, String value)
   {
    return (Pipeline)(super.has(key, value));
   }

  public Pipeline id()
   {
    return (Pipeline)(super.id());
   }
 }

