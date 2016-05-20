import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class UberDataAnalysis {

	public static String Date_to_Day(String input_date) {
		// DateFormat format1 = new SimpleDateFormat("MM/dd/yyyy",Locale.US);
		SimpleDateFormat format1 = new SimpleDateFormat("MM/dd/yyyy");
		Date dt1 = null;
		try {
			dt1 = format1.parse(input_date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DateFormat format2 = new SimpleDateFormat("EE");
		String finalDay = format2.format(dt1);
		// System.out.println(finalDay);
		return finalDay;
	}

	public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
		// private final static IntWritable one = new IntWritable(1);
		// private Text word = new Text();

		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

			int Count = 0;
			String Day = null;
			String Dispatchingnumber = null;
			String Key = null;
			String Value = null;
			String Trips = null;
			int IsDay = 0, Is_Dispatchingnumber = 0, found = 0;

			String line = value.toString();

			// System.out.println(line);

			for (String retval : line.split(",")) {
				Count = Count + 1;
				if (Count == 1) {
					Dispatchingnumber = retval;
					// System.out.println(retval);
					Is_Dispatchingnumber = 1;
				}

				if ((Count == 2) && (Is_Dispatchingnumber == 1)) {
					String pattern = "(\\d+)/(\\d+)/(\\d+)";
					Pattern p = Pattern.compile(pattern);
					Matcher m = p.matcher(retval);
					if (m.find()) {
						Day = UberDataAnalysis.Date_to_Day(retval);
						// System.out.println("day " +Day );
						IsDay = 1;
					}
				}

				if ((Count == 4) && (IsDay == 1) && (Is_Dispatchingnumber == 1)) {
					Trips = retval;
					// System.out.println(Trips);
					found = 1;
				}

				if (found == 1) {
					Key = Dispatchingnumber + " " + Day;
					Value = Trips;
					System.out.println(Key + "	" + Value);
					// System.out.println(retval);
					
					context.write(new Text(Key), new IntWritable(Integer.parseInt(Value)));
				}

			}

			
		}
	}

	// Reducer function after collecting the intermediate data count the values
	// according to each state
	public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {
		public void reduce(Text key, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
			int sum = 0;

			for (IntWritable val : values) {

				sum = sum + val.get();

			}
			String Test = "Null";

			Test = key.toString();
			// if (Test.matches("Female1") ){
			context.write(new Text(Test), new IntWritable(sum));
			// }

		}
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		// Configuration conf = new Configuration();

		Job job = new Job();
		job.setJarByClass(UberDataAnalysis.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);

		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.waitForCompletion(true);
	}
}