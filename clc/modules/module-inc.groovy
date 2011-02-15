import java.io.*;
import org.apache.tools.ant.DemuxOutputStream;
antTarget=project.properties.antTarget
def x = new ByteArrayOutputStream()
def ant = new AntBuilder()
ant.project.getBuildListeners().each {
  it.setOutputPrintStream(new PrintStream(x))
}
properties.modules.eachLine{
  println ( "CALL-MODULE-TARGET ${it} ${antTarget}" )
  ant.ant(dir:"modules/${it}",inheritall:'false'){
    target(name:"${antTarget}")
  }
}
println x.toString()