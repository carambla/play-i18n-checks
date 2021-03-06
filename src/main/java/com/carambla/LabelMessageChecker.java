package com.carambla;

import java.io.*;
import java.util.*;

/**
 *
 * Does not detect label templates that are used as a exception (e.g.: throw new errorException("error",code);
 * Does not detect label templates that are generated as a list with a groovy template
 */
public class LabelMessageChecker {

    private static final char SEPARATOR = File.separatorChar;

    private Configuration configuration;

    public BufferedReader eng;
    public BufferedReader fra;
    public BufferedReader nl;

    public LabelMessageChecker(){

    }

    public static void main(String[] args)  throws IOException{
        System.out.println(System.getProperty("user.dir"));
        File userDir = new File(System.getProperty("user.dir"));

        File configurationFile = new File(userDir.getPath() + SEPARATOR + "application.conf");

        ConfigurationReader reader = new ConfigurationReader();
        reader.writeDefaultConfigurationIfMissing(configurationFile);
        Configuration configuration = reader.read(configurationFile);

        new LabelMessageChecker().run(configuration);
    }

    public Result run(Configuration configuration)throws IOException{
        this.configuration = configuration;

        File playAppDirectory = new File(configuration.homeFolder.getPath() + SEPARATOR + "app");
        if(!playAppDirectory.exists()){
            throw new RuntimeException("App directory missing: " + playAppDirectory.getPath());
        }

        if(!playAppDirectory.isDirectory()){
            throw new RuntimeException(playAppDirectory.getPath() + " is not a directory");
        }

        Result result = new Result();
        searchThroughDirectory(result, playAppDirectory);
        searchForUnusedLabel(result);
        return result;
    }
    public Map<String,File> translateMessages(Configuration configuration) throws IOException {
        this.configuration = configuration;

        File playAppDirectory = new File(configuration.homeFolder.getPath() + SEPARATOR + "app");
        if(!playAppDirectory.exists()){
            throw new RuntimeException("App directory missing: " + playAppDirectory.getPath());
        }

        if(!playAppDirectory.isDirectory()){
            throw new RuntimeException(playAppDirectory.getPath() + " is not a directory");
        }

        return TranslateMessageFiles();
    }

    private Map<String,File> TranslateMessageFiles() throws IOException {
        getMessagesFiles();
        Map<String,File> map = new TreeMap<String,File>();
        File file = new File(configuration.homeFolder.getPath()+ SEPARATOR + "conf" + SEPARATOR + "messages.translated");
        BufferedWriter bf = new BufferedWriter(new FileWriter(file));
        TranslateMassage(bf, eng);
        map.put("messages.translated", file);
        bf = new BufferedWriter(new FileWriter(new File(configuration.homeFolder.getPath()+ SEPARATOR + "conf" + SEPARATOR + "messages.fra.translated")));
        TranslateMassage(bf, fra);
        map.put("messages.fra.translated",file);
        bf = new BufferedWriter(new FileWriter(new File(configuration.homeFolder.getPath()+ SEPARATOR + "conf" + SEPARATOR + "messages.nl.translated")));
        TranslateMassage(bf, nl);
        map.put("messages.nl.translated",file);

        return map;
    }
    private void TranslateMassage(BufferedWriter bf,BufferedReader br) throws IOException {
        String buffer = br.readLine();
        String content = "";
        int i;
        int j;

        while(buffer!=null){
            while(buffer.contains("[") && buffer.contains("]")){
                i = buffer.indexOf("[");
                j = buffer.indexOf("]")+1;
                content += buffer.substring(0,i);
                content += ("{" + "0" + "}");
                buffer = buffer.substring(j,buffer.length());

            }
            content += buffer.substring(0,buffer.length()) + "\n";

            buffer = br.readLine();
        }
        bf.write(content);
        bf.flush();
        bf.close();

    }
    public File translateRoute(Configuration configuration) throws IOException {
        this.configuration = configuration;

        File playAppDirectory = new File(configuration.homeFolder.getPath() + SEPARATOR + "app");
        if(!playAppDirectory.exists()){
            throw new RuntimeException("App directory missing: " + playAppDirectory.getPath());
        }

        if(!playAppDirectory.isDirectory()){
            throw new RuntimeException(playAppDirectory.getPath() + " is not a directory");
        }

        return TranslateRouteFile();
    }

    private File TranslateRouteFile() throws IOException {
        File routeFile = new File(configuration.homeFolder.getPath()+ SEPARATOR + "conf" + SEPARATOR + "routes");
        BufferedReader br = new BufferedReader(new FileReader(routeFile));
        File file = new File(configuration.homeFolder.getPath()+ SEPARATOR + "conf" + SEPARATOR + "routes.translated");
        BufferedWriter bf = new BufferedWriter(new FileWriter(file));

        String buffer = br.readLine();
        String buffer2 = "";
        String content = "";
        int i;
        int j;
        int k;

        while(buffer!=null){
            if(!(buffer.startsWith("#") || buffer.startsWith(" #") || (buffer.trim().isEmpty()))){
                k = buffer.indexOf("/");
                buffer2 = buffer.substring(buffer.indexOf(" ",k));
                buffer2 = buffer2.trim();
                buffer = buffer.substring(0,buffer.indexOf(buffer2));

                content += buffer + "controllers." + buffer2 + "\n";
            }else{
                content += buffer.substring(0,buffer.length()) + "\n";
            }
            buffer = br.readLine();
        }
        bf.write(content);
        bf.flush();
        bf.close();
        return file;
    }

    public void searchThroughDirectory (Result result, File dir) throws IOException {
        for (File file : dir.listFiles()){
            if (file.isDirectory()){
                searchThroughDirectory(result, file);
            }else{
                //if(file.getName().endsWith(".html")){
                    readFile(result, file);
                //}
            }
        }
    }

    private boolean readFile(Result result, File f) throws IOException {
        boolean notFound = false;
        String buffer;
        // System.out.println(f.getPath());
        BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
        //read the userDir till end
        boolean readFile = true;
        while(readFile){
                buffer = bufferedReader.readLine();
                // read the line until te end
                while (buffer != null) {
                    if (buffer.contains("&{\'") || buffer.contains("Messages.get(\"") || buffer.contains("#{errors [\'") || (buffer.contains("#{") && buffer.contains("/}") && (buffer.substring(1).contains("\'")) ) ){
                        int i = 1000000;
                        int j = 1000000;
                        if (buffer.contains("&{\'")) {
                            i = buffer.indexOf('&') + 3;
                            j = buffer.indexOf('\'', i);
                        }
                        if (buffer.contains("Messages.get")) {
                            if (i > (buffer.indexOf("Messages.get") + 14)) {
                                i = buffer.indexOf("Messages.get") + 14;
                                j = buffer.indexOf("\"", i);
                            }


                        }
                        if(buffer.contains("#{errors [\'")){
                            if(i>(buffer.indexOf("#{errors [\'") + 11)){
                                i = buffer.indexOf("#{errors [\'") + 11;
                                buffer = "#{errors [\'" + buffer.substring(i);
                                i = buffer.indexOf("[\'") + 2;
                                j = buffer.indexOf("\'",i);
                                buffer = buffer.substring(0 , j+2) + "#{errors [" + buffer.substring(j + 2);
                            }

                        }
                        if (buffer.contains("#{") && buffer.contains("/}") && !buffer.contains("#{errors [")){
                            if(i>(buffer.indexOf("#{"))){
                                boolean noComma = true;
                                boolean commaIsBefore = true;
//                                        System.out.println("before: " + buffer);
                                i = buffer.indexOf("#{");
                                j = buffer.indexOf("/}", i);
                                if(buffer.contains(":") && buffer.contains(",")){
                                    commaIsBefore = buffer.indexOf(",") < buffer.indexOf(":");
                                    noComma = false;
                                }

                                if(!commaIsBefore || !buffer.contains("\'")){
                                    buffer = "STOP" + buffer.substring(j);
                                    i = 0;
                                    j = 4;
                                }
                                else{
                                    if(commaIsBefore || noComma){
                                        i = buffer.indexOf("\'") + 1;
                                        j = buffer.indexOf("\'", i);
//                                                System.out.println(i + " " +j);
                                    }

                                }
//                                        System.out.println("after: " + buffer);
                            }


                        }

                        String message = buffer.substring(i, j);
                        System.out.println(message);

                        if (!configuration.readException.contains(message)) {

                            // FIXME why do this multiple times? -> Different variable of result get modified
                            getMessagesFiles();
                            try {
                                while (true) {
                                    if (eng.readLine().contains(message)) {
                                        result.foundEng.add(message);
                                        break;
                                    }
                                }
                            } catch (Exception m) {
                                result.notFoundInTemplateEng.add(message);
                                notFound = true;
                            }
                            try {
                                while (true) {
                                    if (fra.readLine().contains(message)) {
                                        result.foundFra.add(message);
                                        break;
                                    }
                                }
                            } catch (Exception m) {
                                result.notFoundInTemplateFra.add(message);
//                                        System.out.println("not in fra: " + message);
                                notFound = true;
                            }
                            try {
                                while (true) {
                                    if (nl.readLine().contains(message)) {
                                        result.foundNl.add(message);
                                        break;
                                    }
                                }
                            } catch (Exception m) {
                                result.notFoundInTemplateNl.add(message);
//                                        System.out.println("not in nl: " + message);
                                notFound = true;
                            }
                        }
                        buffer = buffer.substring(j);
                    }

                    buffer = bufferedReader.readLine();


                }

                if(notFound){
                    result.filesChecked.add(f.getPath());
//                            System.out.println("userDir checked: "+ f.getPath());
//                            System.out.println(e);
                }
                notFound = false;
                readFile = false;
        }
        return notFound;
    }

    public void searchForUnusedLabel(Result result) throws IOException {

        getMessagesFiles();
        String label;
        String subLabel;
        while (true) {
            try {
                label = eng.readLine();
                if (label.contains("=") && !label.contains("#")){
                    subLabel = label.substring(0,label.indexOf("=")).trim();
                    if (!result.foundEng.contains(subLabel)) {
                        if(configuration.unDetectables.contains(subLabel)){
                            result.foundEng.add(subLabel);
                        }
                        else{
                            if(!configuration.readException.contains(subLabel)){
                                result.notFoundEng.add(subLabel);
                            }
                        }

                    }
                    result.totalEng.add(subLabel);
                }
            } catch (NullPointerException m) {
                for(String s: configuration.unDetectables){
                    if (!result.totalEng.contains(s)) {
                        result.notFoundInTemplateEng.add(s);
                    }
                }

                break;
            }
        }


        while (true) {
            try {

                label = fra.readLine();
                if (label.contains("=") && !label.contains("#")) {
                    subLabel = label.substring(0,label.indexOf("=")).trim();
                    if (!result.foundFra.contains(subLabel)) {
                        if(configuration.unDetectables.contains(subLabel)){
                            result.foundFra.add(subLabel);
                        }
                        else{
                            if(!configuration.readException.contains(subLabel)){
                                result.notFoundFra.add(subLabel);
                            }
                        }

                    }
                    result.totalFra.add(subLabel);
                }


            } catch (NullPointerException m) {
                for(String s: configuration.unDetectables){
                    if (!result.totalFra.contains(s)) {
                        result.notFoundInTemplateFra.add(s);

                    }
                }
                break;
            }
        }
        while (true) {
            try {
                label = nl.readLine();
                if (label.contains("=") && !label.contains("#")) {
                    subLabel = label.substring(0,label.indexOf("=")).trim();
                    if (!result.foundNl.contains(subLabel)) {
                        if(configuration.unDetectables.contains(subLabel)){
                            result.foundNl.add(subLabel);
                        }
                        else{
                            if(!configuration.readException.contains(subLabel)){
                                result.notFoundNl.add(subLabel);
                            }
                        }

                    }

                    result.totalNl.add(subLabel);
                }


            } catch (NullPointerException m) {
                for(String s: configuration.unDetectables){
                    if (!result.totalNl.contains(s)) {
                        result.notFoundInTemplateNl.add(s);
                    }
                }
                break;
            }
        }
    }

    public void getMessagesFiles() throws IOException {
        eng = new BufferedReader(new FileReader(new File(configuration.homeFolder.getPath()+ SEPARATOR + "conf" + SEPARATOR + "messages")));
        fra = new BufferedReader(new FileReader(new File(configuration.homeFolder.getPath()+ SEPARATOR + "conf" + SEPARATOR + "messages.fr")));
        nl = new BufferedReader(new FileReader(new File(configuration.homeFolder.getPath()+ SEPARATOR + "conf" + SEPARATOR + "messages.nl")));
    }



    public static class Result {
        //total = templates
        public Set<String> totalEng = new TreeSet<String>();
        public Set<String> totalFra = new TreeSet<String>();
        public Set<String> totalNl = new TreeSet<String>();
        //NotFound =  (templates - (foundLabels - unDetectables - readExceptions))
        //Notfound = total - found
        public Set<String> notFoundEng = new TreeSet<String>();
        public Set<String> notFoundFra = new TreeSet<String>();
        public Set<String> notFoundNl = new TreeSet<String>();
        //notFoundInTemplates = ((foundLabels - readExceptions) + unDectectables) - templates
        public Set<String> notFoundInTemplateEng = new TreeSet<String>();
        public Set<String> notFoundInTemplateFra = new TreeSet<String>();
        public Set<String> notFoundInTemplateNl = new TreeSet<String>();
        //found = (templates - ((foundLabels - readExceptions) + unDetectables)
        //found = total - notFound
        public Set<String> foundEng = new TreeSet<String>();
        public Set<String> foundFra = new TreeSet<String>();
        public Set<String> foundNl = new TreeSet<String>();

        public List<String> filesChecked = new ArrayList<String>();

    }

}
