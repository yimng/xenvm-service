#
# This ProGuard configuration file illustrates how to process applications.
# Usage:
#     java -jar proguard.jar @vgate_service.pro
#



# Specify the input jars, output jars, and library jars.


-injars 'target/vgate_service.jar'
-outjars 'artifacts/vgate_service.jar'

-printmapping 'artifacts/vgate_service.map'

-libraryjars 'C:\Program Files\Java\jre7\lib\rt.jar'
-libraryjars 'C:\Program Files\Java\jre7\lib\jce.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/org/apache/xmlrpc/xmlrpc-client/3.1.3/xmlrpc-client-3.1.3.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/org/apache/xmlrpc/xmlrpc-common/3.1.3/xmlrpc-common-3.1.3.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/org/apache/ws/commons/util/ws-commons-util/1.0.2/ws-commons-util-1.0.2.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/commons-logging/commons-logging/1.2/commons-logging-1.2.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/commons-codec/commons-codec/1.9/commons-codec-1.9.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/org/apache/httpcomponents/httpcore/4.4.4/httpcore-4.4.4.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/org/apache/httpcomponents/httpclient/4.5.1/httpclient-4.5.1.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/log4j/log4j/1.2.17/log4j-1.2.17.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/org/slf4j/slf4j-log4j12/1.7.12/slf4j-log4j12-1.7.12.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/org/slf4j/slf4j-api/1.7.12/slf4j-api-1.7.12.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/citrix/xen-api/6.5/xen-api-6.5.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/com/jcraft/jsch/0.1.53/jsch-0.1.53.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/commons-io/commons-io/1.3.2/commons-io-1.3.2.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/com/google/guava/guava/19.0/guava-19.0.jar'
-libraryjars 'E:/apache-maven-3.1.1/repository/mysql/mysql-connector-java/5.1.30/mysql-connector-java-5.1.30.jar'



# Save the obfuscation mapping to a file, so you can de-obfuscate any stack
# traces later on. Keep a fixed source file attribute and all line number
# tables to get line numbers in the stack traces.
# You can comment this out if you're not interested in stack traces.

-dontshrink

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Preserve all annotations.

-keepattributes *Annotation*

# You can print out the seeds that are matching the keep options below.

#-printseeds out.seeds

# Preserve all public applications.

-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# Preserve all native method names and the names of their classes.

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Preserve the special static methods that are required in all enumeration
# classes.

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Explicitly preserve all serialization members. The Serializable interface
# is only a marker interface, so it wouldn't save them.
# You can comment this out if your application doesn't use serialization.
# If your code contains serializable classes that have to be backward 
# compatible, please refer to the manual.

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Your application may contain more items that need to be preserved; 
# typically classes that are dynamically created using Class.forName:
-keepattributes Exceptions,InnerClasses
-keep public interface com.halsign.vgate.service.LicenseService
-keep public class * implements com.halsign.vgate.service.LicenseService

-keep public interface com.halsign.vgate.service.NetworkService
-keep public class * implements com.halsign.vgate.service.NetworkService

-keep public interface com.halsign.vgate.service.TemplateService
-keep public class * implements com.halsign.vgate.service.TemplateService

-keep public interface com.halsign.vgate.service.VersionService
-keep public class * implements com.halsign.vgate.service.VersionService

-keep public interface com.halsign.vgate.service.VmService
-keep public class * implements com.halsign.vgate.service.VmService

-keep public interface com.halsign.vgate.service.HostService
-keep public class * implements com.halsign.vgate.service.HostService

-keep public class com.halsign.vgate.DAO.DAOManager
-keep public class com.halsign.vgate.thread.Worker
-keep public class com.halsign.vgate.VgateException {
    *;
}
-keep public class com.halsign.vgate.util.VgateConnectionPool {
	*;
}
-keep public class com.halsign.vgate.VgateMessage {
    *;
}
-keep public class com.halsign.vgate.License
-keep public class com.halsign.vgate.IpAddress
-keep public class com.halsign.vgate.VgateTask {
    *;
}
-keep public class com.halsign.vgate.Template {
	*;
}
-keep public class com.halsign.vgate.util.VgateMessageConstants {
	*;
}
-keep public class com.halsign.vgate.util.VgateConstants {
    *;
}
-keep public class com.halsign.vgate.util.VgateConnectionPool$VgateConnection {
	*;
}

-keepclassmembers public class com.halsign.vgate.IpAddress {
    public ** *(...);
    public void *(...);
}
-keepclassmembers public class com.halsign.vgate.License {
    *;
}
-keep public class com.halsign.vgate.spec.TemplateSpec {
	*;
}
-keep public class com.halsign.vgate.spec.VmSpec {
	*;
}
-keep public class com.halsign.vgate.BalancePolicy {
	*;
}
-keep public class com.halsign.vgate.Policy {
	*;
}
-keep public interface com.halsign.vgate.HalsignCallBack {
	*;
}

-keep public class com.halsign.vgate.util.VgateUtil
-keepclassmembers public class com.halsign.vgate.util.VgateUtil {
	public static boolean isSRHasEnoughSpace(com.halsign.vgate.util.VgateConnectionPool$VgateConnection, com.xensource.xenapi.VM, com.xensource.xenapi.SR);
	public static void disconnect(com.halsign.vgate.util.VgateConnectionPool$VgateConnection);
	public static long getTemplateSpace(com.halsign.vgate.util.VgateConnectionPool$VgateConnection, com.xensource.xenapi.VM);
	public static java.lang.String findTemplate(java.lang.String, java.lang.String);
}

-keepclassmembers public class com.halsign.vgate.DAO.DAOManager {
    public static com.halsign.vgate.DAO.DAOManager getInstance();
    public static void setDataSource(javax.sql.DataSource);
}

-keepclassmembers public class com.halsign.vgate.thread.Worker {
	public static void start(com.halsign.vgate.HalsignCallBack);
	public static void stop();
}

-keepclassmembers public class com.halsign.vgate.util.VgateConnectionPool {    
    public static com.halsign.vgate.util.VgateConnectionPool getInstance();
    public com.halsign.vgate.util.VgateConnectionPool$VgateConnection getConnect(java.lang.String, java.lang.String, java.lang.String, int, int);
    public com.halsign.vgate.util.VgateConnectionPool$VgateConnection connect(java.lang.String, java.lang.String, java.lang.String, int, int);
}

-keepclassmembers public interface com.halsign.vgate.service.LicenseService {
    public com.halsign.vgate.VgateMessage applyLicense(com.halsign.vgate.util.VgateConnectionPool$VgateConnection, java.lang.String, com.halsign.vgate.License);
}
-keepclassmembers public interface com.halsign.vgate.service.NetworkService {
    *;
}
-keepclassmembers public interface com.halsign.vgate.service.TemplateService {
    *;
}
-keepclassmembers public interface com.halsign.vgate.service.VersionService {
    *;
}
-keepclassmembers public interface com.halsign.vgate.service.VmService {
    *;
}
-keepclassmembers public interface com.halsign.vgate.service.HostService {
    *;
}

