package Readevent;
    
	import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
	        
	import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
	        
	public class Readdata {
	        
	 public static class GetInfoMap extends Mapper<LongWritable, Text, Text, DoubleArrayWritable> {
	    private final static IntWritable one = new IntWritable(1);
	    private Text event_id = new Text();
	    
	    private double[] coordinate = new double[2];
	    private double latitude;;
	    private double longtitude;;

	    private boolean flag_event_id =false;
	    private boolean flag_latitude =false;
	    private boolean flag_longtitude =false;

	    
	        
	    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
	        String line = value.toString();
	        String delimiter = "\\|";
	        String[] column = line.split(delimiter);
	        for (int i=0; i < column.length; i++)
	        {
	        	if (column[i].matches("E\\d-\\d{3}-\\d{9}-\\d"))        		
	        	{
	        		event_id.set(column[i]);
	        		flag_event_id=true;
	        		continue;
	        	}
	        	
	        	if (column[i].matches("\\d+.\\d+")&&(!flag_latitude))
	        	{
	        		latitude=(Double.parseDouble(column[i]));
	        		flag_latitude=true;
	        		continue;
	        	}
	        	if (column[i].matches("\\d+.\\d+")&&(!flag_longtitude))
	        	{
	        		longtitude=(Double.parseDouble(column[i]));
	        		flag_longtitude=true;
	        		continue;
	        	}
	        	
	        	//get one complete tuple
	        	if (flag_event_id&&flag_longtitude&&flag_latitude) break; 
	        	

	        }
	        coordinate[0]= latitude;
	        coordinate[1]=longtitude;
	        if (flag_event_id&&flag_longtitude&&flag_latitude) 
			{
				context.write(event_id, new DoubleArrayWritable(coordinate));
			    flag_event_id =false;
			    flag_latitude =false;
			    flag_longtitude =false;
			}

	        	
	        
	    }
	 } 
	 
	 public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
		    private final static IntWritable one = new IntWritable(1);
		    private Text word = new Text();
		        
		    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
		        String line = value.toString();
		        String delimiter = "\\|";
		        String[] column = line.split(delimiter);
		        for (int i=0; i < column.length; i++)
		        {
		        	if (column[i].matches("E\\d-\\d{3}-\\d{9}-\\d")
		        			||column[i].matches("\\d+.\\d+") )
		        	{
		        		word.set(column[i]);
			        	context.write(word, one);
		        	}

		        }
		    }
		 } 
	 
	        
	 public static class Reduce extends Reducer<Text, DoubleArrayWritable, Text, Text> {

	    public void reduce(Text key, Iterable<DoubleArrayWritable> values, Context context) 
	      throws IOException, InterruptedException {
	        int sum = 0;
	        for (DoubleArrayWritable val : values) {
	          //  sum += val[].get();
	        	context.write(key, new Text(val.toString()));
	        }
	        
	    }
	 }
	        
	 public static void main(String[] args) throws Exception {
		 //http://irwenqiang.iteye.com/blog/1542355
		 
	    Configuration conf = new Configuration();
	        
	    Job job = new Job(conf, "Readdata");
	    //缺上面这一句导致不能运行得很好
	    job.setJarByClass(Readdata.class);
	    
        // Set the outputs for the Map
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(DoubleArrayWritable.class);

        // Set the outputs for the Job
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
//	    job.setOutputKeyClass(Text.class);
//	    job.setOutputValueClass(IntWritable.class);
	    
	    
	    job.setMapperClass(GetInfoMap.class);
	    job.setReducerClass(Reduce.class);
	        
	    job.setInputFormatClass(TextInputFormat.class);
	    job.setOutputFormatClass(TextOutputFormat.class);
	        
	    FileInputFormat.addInputPath(job, new Path(args[0]));
	    FileOutputFormat.setOutputPath(job, new Path(args[1]));
	        
	    job.waitForCompletion(true);

	    /*
	    Configuration conf1 = new Configuration();
        
	    Job job1 = new Job(conf1, "Readdata");
	    //缺上面这一句导致不能运行得很好
	    job1.setJarByClass(Readdata.class);
	    job1.setOutputKeyClass(Text.class);
	    job1.setOutputValueClass(IntWritable.class);
	    
	    
	    job1.setMapperClass(Map.class);
	    job1.setReducerClass(Reduce.class);
	        
	    job1.setInputFormatClass(TextInputFormat.class);
	    job1.setOutputFormatClass(TextOutputFormat.class);
	        
	    FileInputFormat.addInputPath(job1, new Path(args[1]));
	    FileOutputFormat.setOutputPath(job1, new Path(args[2]));
	        
	    job1.waitForCompletion(true);
	    
	    */
	    
	 }
	        
	}
	
	// I do not now if it is needed.
	class DoubleArrayWritable implements Writable {
	    private double[] data;

	    public DoubleArrayWritable() {

	    }

	    public DoubleArrayWritable(double[] data) {
	        this.data = data;
	    }

	    public double[] getData() {
	        return data;
	    }

	    public void setData(double[] data) {
	        this.data = data;
	    }

	    public void write(DataOutput out) throws IOException {
	        int length = 0;
	        if(data != null) {
	            length = data.length;
	        }

	        out.writeInt(length);

	        for(int i = 0; i < length; i++) {
	            out.writeDouble(data[i]);
	        }
	    }

	    public void readFields(DataInput in) throws IOException {
	        int length = in.readInt();

	        data = new double[length];

	        for(int i = 0; i < length; i++) {
	            data[i] = in.readDouble();
	        }
	    }
	    
	    public String toString() {
	        if (data.length == 0) {
	          return "";
	        }

	        StringBuilder sb = new StringBuilder();
	        for (double d : data) {
	            sb.append(d).append("/t");
	        }

	        //trim the trailing space
	        sb.setLength(sb.length() - 1);
	        return sb.toString();
	    }
	}