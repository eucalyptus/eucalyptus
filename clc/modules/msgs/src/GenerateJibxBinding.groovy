import java.lang.reflect.Modifier;

def bindingFile = new File("resources/msgs-binding.xml");
bindingFile.write("");
//Enumeration<JarEntry> pathList = (new JarFile("lib/eucalyptus.jar")).entries();
def pathList = new File("build/edu/ucsb/eucalyptus/msgs/").list().toList();
def classList = [];
pathList.each({
              def hithere = it.replace('.class', '');
              classList << "edu.ucsb.eucalyptus.msgs.$hithere";
              });

def binding = {
  ns ->
  def cleanNs = ns.replaceAll('(http://)|(/$)', "").replaceAll("[./-]", "_");
  bindingFile.append "<binding xmlns:euca=\"$ns\" name=\"$cleanNs\">"
  bindingFile.append "  <namespace uri=\"$ns\" default=\"elements\" prefix=\"euca\"/>"
}

def baseMapping = {
  name, className ->
  bindingFile.append "<mapping abstract=\"true\" class=\"$className\">";
}

def childMapping = {
  name, className, extendsName, isAbstract ->
  bindingFile.append "<mapping "
  if ( isAbstract ) bindingFile.append "abstract=\"true\""
  else bindingFile.append "name=\"$name\""
  bindingFile.append " extends=\"$extendsName\" class=\"$className\" >";
  bindingFile.append "    <structure map-as=\"$extendsName\"/>"
}

def valueBind = {
  name ->
  bindingFile.append "    <value style=\"element\" name=\"$name\" field=\"$name\" usage=\"optional\"/>";
}

def typeBind = {
  name, type ->
  bindingFile.append "    <structure name=\"$name\" field=\"$name\" map-as=\"$type\" usage=\"optional\"/>";
}

def stringCollection = {
  name ->
  bindingFile.append "    <structure name=\"$name\" usage=\"optional\"><collection factory=\"org.jibx.runtime.Utility.arrayListFactory\" field=\"$name\" item-type=\"java.lang.String\" usage=\"required\">";
  bindingFile.append "<structure name=\"item\"><value name=\"entry\"/></structure></collection></structure>";
}

def typedCollection = {
  name, itemType ->
  bindingFile.append "    <structure name=\"$name\" usage=\"optional\"><collection field=\"$name\" factory=\"org.jibx.runtime.Utility.arrayListFactory\" usage=\"required\">";
  bindingFile.append "<structure name=\"item\" map-as=\"$itemType\"/></collection></structure>";
}


binding("http://msgs.eucalyptus.ucsb.edu");
classList.each({
               Class itsClass = Class.forName(it);

               if ( itsClass.getSuperclass().getSimpleName().equals("Object") )
               baseMapping(itsClass.getSimpleName(), itsClass.getName());
               else if ( itsClass.getSuperclass().getSimpleName().equals("EucalyptusData") )
               childMapping(itsClass.getSimpleName().replaceAll("Type", ""), itsClass.getName(), itsClass.getSuperclass().getName(), true);
               else
               childMapping(itsClass.getSimpleName().replaceAll("Type", ""), itsClass.getName(), itsClass.getSuperclass().getName(), false);

               def fieldList = itsClass.getDeclaredFields().findAll({Modifier.isPrivate(it.getModifiers())})
               fieldList.each({
                              Class itsType = it.getType();
                              if ( itsType.getSuperclass().equals(edu.ucsb.eucalyptus.msgs.EucalyptusData) )
                              typeBind(it.getName(), itsType.getName());
                              else if ( it.getType().equals(java.util.ArrayList.class) )
                              {
                              if ( it.getGenericType().getActualTypeArguments()[ 0 ].equals(java.lang.String) )
                              stringCollection(it.getName());
                              else
                              typedCollection(it.getName(), it.getGenericType().getActualTypeArguments()[ 0 ].getName());
                              }
                              else
                              valueBind(it.getName());
                              /** date     **/
                              /** arraylist     **/
                              /** other     **/
                              })
               bindingFile.append("</mapping>");

               })
bindingFile.append("</binding>");



