import java.io.*;
import org.apache.tools.ant.DemuxOutputStream;
antTarget=project.properties.antTarget
def ant = new AntBuilder()
properties.modules.eachLine{
  println ( "CALL-MODULE-TARGET ${it} ${antTarget}" )
  ant.ant(dir:"modules/${it}",inheritall:'false'){
    target(name:"${antTarget}")
  }
}
