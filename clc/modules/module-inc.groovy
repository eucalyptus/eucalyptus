import java.io.*;
import org.apache.tools.ant.DemuxOutputStream;
import com.google.common.io.Files;

def moduleBasePath = "${project.baseDir}/modules";
def moduleDirs = new File(moduleBasePath).listFiles( { it.isDirectory() } as FileFilter ).collect{ it.getName() }
def modulesList = new File("${moduleBasePath}/module-inc.order");
def antTarget=project.properties.antTarget
def ant = new AntBuilder()
def modulesBuild = []
def modulesIgnore = []
def buildOrder = []
def doBuild = { module ->
  if ( new File("${moduleBasePath}/${module}/build.xml").exists() ) {
    println ( "CALL-MODULE-TARGET ${module} ${antTarget}" )
    ant.ant(dir:"modules/${module}",inheritall:'false'){
      target(name:"${antTarget}")
    }
  } else {
    println ( "SKIP-MODULE-TARGET ${module} ${antTarget}" )
  }
}

modulesList.eachLine{
  if ( it.startsWith("#") ) {
    moduleDirs.remove(it.substring(1).trim())
  } else {
    moduleDirs.remove(it)
    buildOrder += it;
  }
}
buildOrder.addAll( moduleDirs )
println "==== BUILD ORDER ===="
buildOrder.each{ print "=> ${it} "}
println "\n====================="
buildOrder.each{ doBuild(it) }
