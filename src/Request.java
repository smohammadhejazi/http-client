import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * This class represents a http request. It holds all the information about
 * the request.
 */
public class Request implements Serializable
{
    //URL of the request
    private URL url;
    //Method of the request, default is GET
    private String method = "GET";
    //Response headers of the request
    private Map<String, List<String>> responseHeaders;
    //Request Headers of the request
    private Map<String, List<String>> requestHeaders;
    //FormData of the request
    private Map<String, List<String>> formData;
    //The date of the request
    private String date;
    //The option arguments of the request
    private ArrayList<String> options;
    //Response body as byte array
    private byte[] responseBody;
    //Response status, response code + response massage
    private String responseStatus;
    //Response time of the request
    private long responseTime = -1;

    /**
     * Create a new request
     * @param url URL of the request
     * @param options Option arguments of the request
     */
    public Request(URL url, ArrayList<String> options)
    {

        this.url = url;
        this.options = options;
        if (!this.setMethod())
        {
            System.err.println("Error: Not a valid method");
            return;
        }

        sendRequest();

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh-mm-ss-SSS");
        String dateTime = formatter.format(now);
        this.date= dateTime;

        if (hasOption("-O") || hasOption("--output"))
        {
            String fileAddress = null;
            fileAddress = hasOption("-O") ? getValueOf("-O") : getValueOf("--output");

            if (fileAddress != null)
                saveResponseBody(fileAddress);
            else
                saveResponseBody(null);
        }

        if (hasOption("-S") || hasOption("--save"))
        {
            saveRequest(dateTime);
        }

    }

    /**
     * Send the request.
     */
    public void sendRequest()
    {
        try{
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            setHeaders(connection);
            connection.setRequestMethod(method);

            requestHeaders = connection.getRequestProperties();

            if (hasOption("-d") || hasOption("--data"))
            {
                String boundary = System.currentTimeMillis() + "";
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                BufferedOutputStream writer = new BufferedOutputStream(connection.getOutputStream());
                setMultipartFormData(writer, boundary);
            }
            else if (hasOption("-e") || hasOption("--encoded"))
            {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                BufferedOutputStream writer = new BufferedOutputStream(connection.getOutputStream());
                setFormUrlencoded(writer);
            }

            int responseCode = connection.getResponseCode();
            String responseStatus = connection.getResponseCode() + " " +connection.getResponseMessage();
            this.responseStatus = responseStatus;
            System.out.printf("HTTP/1.1 %s%n%n", responseStatus);

            if (responseCode >= 200 && responseCode < 300)
            {
                long startTime = System.currentTimeMillis();
                if (hasOption("-i"))
                {
                    Map<String, List<String>> responseHeaders = connection.getHeaderFields();
                    this.responseHeaders = responseHeaders;
                    for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet())
                    {
                        String entryValue = entry.getValue().toString();
                        String entryKey = entry.getKey();

                        if (entryKey == null)
                            continue;

                        int entryValueLength = entry.getValue().toString().length();
                        System.out.println(entryKey + " : " + entryValue.substring(1, entryValueLength - 1));
                    }
                    System.out.println();
                }

                BufferedInputStream reader = new BufferedInputStream(connection.getInputStream());

                responseBody = reader.readAllBytes();
                if (hasOption("-O") || hasOption("--output"))
                    return;
                System.out.println(new String(responseBody));
                long responseTime = System.currentTimeMillis() - startTime;
                this.responseTime = responseTime;
            }
            else
            {
                System.err.printf("Error: %s request did not work.", method);
            }

        }
        catch (IOException | NullPointerException e) {
            System.err.printf("Error: %s request failed.", method);
        }

    }

    /**
     * Writes the response body to a file.
     * @param fileAddress Name of the file
     */
    public void saveResponseBody(String fileAddress)
    {
        File file = new File("./ResponseOutput");
        file.mkdir();

        if (fileAddress == null)
        {
            fileAddress = "./ResponseOutput/" + "output_" + date;
        }

        try(FileOutputStream os = new FileOutputStream("./ResponseOutput/" + fileAddress))
        {
            os.write(responseBody);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Save the request.
     * @param dateTime Date of the request, it is used in the name of the saved
     *                 file
     */
    public void saveRequest(String dateTime)
    {
        File file = new File("./RequestHistory");
        file.mkdir();

        try (FileOutputStream fs = new FileOutputStream("./RequestHistory/request_" + dateTime))
        {
            ObjectOutputStream os = new ObjectOutputStream(fs);

            os.writeObject(this);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the option arguments has the specified option.
     * @param option Option to be checked
     * @return True if it has the option and false otherwise
     */
    private boolean hasOption(String option)
    {
        for (String s : options)
        {
            if (option.equals(s))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the value of the specified option.
     * @param option Option to get the value of
     * @return The value of the option
     */
    private String getValueOf(String option)
    {
        for (int i = 0; i < options.size(); i++)
        {
            if (options.get(i).equals(option))
            {
                String s = options.get(i+1);
                if (s.charAt(0) == '-')
                    return null;
                else
                    return options.get(i+1);
            }
        }
        return null;
    }

    /**
     * Set the headers for the request
     * @param connection Connection of the request
     */
    public void setHeaders(HttpURLConnection connection)
    {
        if (hasOption("-H") || hasOption("--headers"))
        {
            for (int i = 0; i < options.size(); i++)
            {
                if (options.get(i).equals("-H") ||options.get(i).equals("--header"))
                {
                    String header = options.get(i+1);

                    String[] keyValues = header.split(";");

                    for (String s : keyValues)
                    {
                        String[] keyValue = s.split(":", 2);
                        connection.addRequestProperty(keyValue[0].trim(), keyValue[1].trim());
                    }
                    i++;
                }
            }
        }
    }

    /**
     * Set the multipart form data.
     * @param writer Writer to the outputStream of the request
     * @param boundary Boundary of the multipart form data
     */
    public void setMultipartFormData(BufferedOutputStream writer, String boundary)
    {
        try
        {
            formData = new HashMap<>();
            for (int i = 0; i < options.size(); i++)
            {
                if (options.get(i).equals("-d") || options.get(i).equals("--data"))
                {
                    String body = options.get(i + 1);
                    String[] keyValues = body.split("&");
                    for (String s : keyValues)
                    {
                        String[] keyValue = s.split("=", 2);
                        formData.put(keyValue[0], new ArrayList<String>(Collections.singletonList(keyValue[1])));

                        if (keyValues[0].contains("file"))
                        {
                            File fileToUpload = new File(keyValue[1]);
                            try
                            {
                                FileInputStream inputStream = new FileInputStream(fileToUpload);
                                //
                                BufferedInputStream reader = new BufferedInputStream(new FileInputStream(new File(keyValue[1])));
                                writer.write(("--" + boundary + "\r\n").getBytes());
                                writer.write(("Content-Disposition: form-data; filename=\"" +
                                        (new File(keyValue[0])).getName() +
                                        "\"\r\nContent-Type: Auto" + "\"\r\n\r\n").getBytes());
                                byte[] fileBytes = reader.readAllBytes();
                                writer.write(fileBytes);
                                writer.write(("\r\n").getBytes());
                            }
                            catch (IOException e)
                            {
                                System.err.printf("Error: Could not find the file: %s%n", keyValue[1]);

                            }
                        }
                        else
                        {
                            try
                            {
                                writer.write(("--" + boundary + "\r\n").getBytes());
                                writer.write(("Content-Disposition: form-data; name=\"" + keyValue[0] + "\"\r\n\r\n").getBytes());
                                writer.write((keyValue[1] + "\r\n").getBytes());
                            }
                            catch (IOException e)
                            {
                                System.err.println("Error: Could not write formData");
                            }
                        }
                    }
                    i++;
                }

            }
        }
        catch(ArrayIndexOutOfBoundsException e)
        {
            System.err.println("Error: Illegally formatted form-data.");
        }
        finally
        {
            try
            {
                writer.write(("--" + boundary + "--\r\n").getBytes());
                writer.flush();
                writer.close();
            }
            catch (IOException e)
            {
                System.err.println("Error: Could not write formData");
            }
        }
    }

    /**
     * Set the form url encoded
     * @param writer Writer to the outputStream of the request
     */
    public void setFormUrlencoded(BufferedOutputStream writer)
    {
        try
        {
            boolean first = false;
            for (int i = 0; i < options.size(); i++)
            {
                if (options.get(i).equals("-e") || options.get(i).equals("--encoded"))
                {
                    if (first)
                        writer.write(("&").getBytes());

                    String body = (options.get(i + 1));
                    writer.write(body.getBytes());

                    first = true;
                    i++;
                }
            }

            writer.flush();
            writer.close();
        }
        catch (IOException e)
        {
            System.err.println("Error: Could not write formData");
        }
    }

    /**
     * Set the method of the request.
     * @return True if the the method is valid and false otherwise
     */
    public boolean setMethod()
    {
        if (hasOption("-M"))
        {
            this.method = getValueOf("-M");
        }
        else if (hasOption("--method"))
        {
            this.method = getValueOf("--method");
        }

        return method.equals("GET") ||
                method.equals("POST") ||
                method.equals("PUT") ||
                method.equals("DELETE");
    }

    /**
     * Get the URL of the request.
     * @return URL of the request
     */
    public URL getUrl()
    {
        return url;
    }

    /**
     * Get the method of the request.
     * @return Method of the request
     */
    public String getMethod()
    {
        return method;
    }

    /**
     * Get the option arguments of the request.
     * @return Option arguments of the request
     */
    public ArrayList<String> getOptions()
    {
        return options;
    }

    /**
     * Get the response headers of the request.
     * @return Response headers of the request
     */
    public Map<String, java.util.List<String>> getResponseHeaders()
    {
        return responseHeaders;
    }

    /**
     * Get the request headers of the request.
     * @return Request headers of the request
     */
    public Map<String, List<String>> getRequestHeaders()
    {
        return requestHeaders;
    }

    /**
     * Get the form data of the request.
     * @return Form data of the request
     */
    public Map<String, List<String>> getFormData()
    {
        return formData;
    }

    /**
     * Get the date of the request.
     * @return Date of the request
     */
    public String getDate()
    {
        return date;
    }

    /**
     * Get the response body of the request.
     * @return Response body of the request
     */
    public byte[] getResponseBody()
    {
        return responseBody;
    }

    /**
     * Get the response status (response code + response massage)
     * @return Response status of the request
     */
    public String getResponseStatus()
    {
        return responseStatus;
    }

    /**
     * Get the response time of the request.
     * @return Response time of the request
     */
    public String getResponseTime()
    {
        if (responseTime == -1)
            return null;
        return String.valueOf(responseTime);
    }

    /**
     * Get the response size of the request
     * @return Response size of the request.
     */
    public String getResponseSize()
    {
        if (responseBody == null)
            return null;

        double size = responseBody.length / 1024.0;
        return String.format("%.2fkb", size);
    }
}
