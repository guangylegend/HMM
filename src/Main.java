import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Vector;

public class Main {
	
	private static HashMap<String, Hashtable<String, Integer>> emit = new HashMap<String, Hashtable<String, Integer>>();
	private static HashMap<String, Hashtable<String, Integer>> dict = new HashMap<String, Hashtable<String, Integer>>();
	private static HashMap<String, Hashtable<String, Integer>> arc = new HashMap<String, Hashtable<String, Integer>>();
	private static HashMap<String, Integer> count = new HashMap<String, Integer>();
	
	public static void main(String[] args) throws IOException{
		
		File trainfile = new File("D:\\WSJ_02-21.pos");
		File validfile = new File("D:\\WSJ_24.words");
		BufferedReader brtrain = new BufferedReader(new FileReader(trainfile));
		BufferedReader brvalid = new BufferedReader(new FileReader(validfile));
		
		//redirect valid output
//		PrintStream ps=new PrintStream(new FileOutputStream("D:\\WSJ_out.txt"));  
//      System.setOut(ps); 
        
        //redirect output
        PrintStream ps=new PrintStream(new FileOutputStream("D:\\wsj_24.pos"));  
        System.setOut(ps); 
		
		String laststate = "start";
		arc.put("start", new Hashtable<String, Integer>());
		//arc.put("end", new Hashtable<String, Integer>());
		
		count.put("total",0);
		
		HashMap<String, Integer> hapaxcount = new HashMap<String, Integer>();
		HashMap<String, Integer> singlecount = new HashMap<String, Integer>();
		
		String s = null;
		while((s = brtrain.readLine())!=null){
			String tmp[] = s.split("\t");
			if(tmp.length < 2){
				//handle arc
				if(arc.get(laststate)==null)arc.put(laststate, new Hashtable<String, Integer>());
				
				if(arc.get(laststate).get("end")!=null){
					arc.get(laststate).put("end", arc.get(laststate).get("end")+1);
				}
				else arc.get(laststate).put("end", 1);
				
				//handle count
				if(count.get(laststate)==null)count.put(laststate, 1);
				else count.put(laststate, count.get(laststate)+1);
				count.put("total", count.get("total")+1);
				
				laststate = "start";
			}
			else{
				//handle emit
				if(emit.get(tmp[1])==null)emit.put(tmp[1], new Hashtable<String, Integer>());
	
				if(emit.get(tmp[1]).get(tmp[0])!=null){
					emit.get(tmp[1]).put(tmp[0], emit.get(tmp[1]).get(tmp[0])+1);
				}
				else emit.get(tmp[1]).put(tmp[0], 1);

				
				//handle arc
				if(arc.get(laststate)==null)arc.put(laststate, new Hashtable<String, Integer>());
				
				if(arc.get(laststate).get(tmp[1])!=null){
					arc.get(laststate).put(tmp[1], arc.get(laststate).get(tmp[1])+1);
				}
				else arc.get(laststate).put(tmp[1], 1);
				
				//handle dict
				if(dict.get(tmp[0])==null)dict.put(tmp[0], new Hashtable<String, Integer>());
				
				if(dict.get(tmp[0]).get(tmp[0])!=null){
					dict.get(tmp[0]).put(tmp[1], emit.get(tmp[0]).get(tmp[1])+1);
				}
				else dict.get(tmp[0]).put(tmp[1], 1);
				
				//handle single
				if(singlecount.get(tmp[0])==null)singlecount.put(tmp[0], 1);
				else singlecount.put(tmp[0], singlecount.get(tmp[0])+1);
				
				//handle count
				if(count.get(laststate)==null)count.put(laststate, 1);
				else count.put(laststate, count.get(laststate)+1);
				count.put("total", count.get("total")+1);
				
				laststate = tmp[1];
			}		
		}
		count.put("total", count.get("total")-count.get("start"));

		brtrain.close();
		
		hapaxcount.put("total", 0);
		for(Entry<String, Integer> entry: singlecount.entrySet()){
			if(entry.getValue() == 1){
				String word = entry.getKey();
				for(Entry<String, Integer> cnt: dict.get(word).entrySet()){
					if(hapaxcount.get(cnt.getKey())==null)hapaxcount.put(cnt.getKey(), cnt.getValue());
					else hapaxcount.put(cnt.getKey(), hapaxcount.get(cnt.getKey()) + cnt.getValue());
					hapaxcount.put("total", hapaxcount.get("total") + cnt.getValue());
				}
			}
		}

		
				
		//System.out.println("Complete training...");
		
		//output
//		for(Entry<String, Hashtable<String, Integer>> map1: emit.entrySet())
//		{
//			System.out.println("STATE " + map1.getKey() + " " + count.get(map1.getKey()));
//			for(Entry<String, Integer> map2: map1.getValue().entrySet()){
//				System.out.println("EMIT " + map2.getKey() + "\t" + map2.getValue());
//			}
//		}
//		for(Entry<String, Hashtable<String, Integer>> map1: arc.entrySet())
//		{
//			System.out.println("STATE " + map1.getKey() + " " + count.get(map1.getKey()));
//			for(Entry<String, Integer> map2: map1.getValue().entrySet()){
//				System.out.println("ARC TO " + map2.getKey() + "\t" + map2.getValue());
//			}
//		}
		
		
		//start validation
		Vector<String> typelist = new Vector<String>();
		typelist.add("start");
		for(Entry<String, Hashtable<String, Integer>> map: emit.entrySet())
		{
			typelist.add(map.getKey());
		}	
		typelist.add("end");
		int typenum = emit.keySet().size();
		
		
		Vector<Vector<Double>> viterbi = new Vector<Vector<Double>>();
		Vector<Vector<Integer>> backtrack = new Vector<Vector<Integer>>();
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<String> tags = new ArrayList<String>();
		
		Vector<Double> tmp = new Vector<Double>();
		Vector<Integer> backtmp = new Vector<Integer>();
		for(int i=0;i<=typenum+1;i++){
			tmp.add(-1.0);
			backtmp.add(-1);
			//System.out.println(typelist.get(i));
		}
		tmp.set(0, 1.0);
		viterbi.add(tmp);
		backtrack.add(backtmp);
		
		while((s = brvalid.readLine())!=null){
			if(s.equals("")){
				tmp = new Vector<Double>();
				backtmp = new Vector<Integer>();
				for(int i=0;i<=typenum+1;i++){
					tmp.add(-1.0);
					backtmp.add(-1);
				}
				
				for(int j=0;j<=typenum;j++){
					if(viterbi.lastElement().get(j)!=0){
						double prob = viterbi.lastElement().get(j);
						String formerState = typelist.get(j);
						String curState = "end";
						if(arc.get(formerState).containsKey(curState)){
							prob  = prob * arc.get(formerState).get(curState)/count.get(formerState);
						}
						if(prob > tmp.lastElement()){
							tmp.set(typenum+1,prob);
							backtmp.set(typenum+1, j);
						}
					}
				}
				viterbi.add(tmp);
				backtrack.add(backtmp);
				
				//perform backtrack
				
				int state = typenum+1;
				for(int i=viterbi.size()-1;i>=2;i--){
					tags.add(typelist.get(backtrack.get(i).get(state)));
					state = backtrack.get(i).get(state);
				}
				Collections.reverse(tags);
				
				//output
				
				for(int i=0;i<words.size();i++){
					//if(tags.get(i).equals("RP"))System.out.println(words.get(i));
					System.out.println(words.get(i) + "\t" + tags.get(i));
				}
				System.out.println();
				
				
				
				//reinitialize
				viterbi = new Vector<Vector<Double>>();
				backtrack = new Vector<Vector<Integer>>();
				words = new ArrayList<String>();
				tags = new ArrayList<String>();
				
				tmp = new Vector<Double>();
				backtmp = new Vector<Integer>();
				for(int i=0;i<=typenum+1;i++){
					tmp.add(-1.0);
					backtmp.add(-1);
				}
				tmp.set(0, 1.0);
				viterbi.add(tmp);
				backtrack.add(backtmp);
				
			}
			else{
				words.add(s);			
				tmp = new Vector<Double>();
				tmp.add(-1.0);
				backtmp = new Vector<Integer>();
				backtmp.add(-1);
				
				//unknown word
				boolean unknownflag = true;
				for(int i=1;i<=typenum;i++){
					if(emit.get(typelist.get(i)).containsKey(s)){
						unknownflag = false;
						break;
					}
				}
				
				if(unknownflag){
					//System.out.println("unknown word detected: " + s);
					
					double tmpmax = -1.0;
					int indextmp = -1;
					for(int i=0;i<=typenum;i++){
						if(viterbi.lastElement().get(i)!=-1.0 && viterbi.lastElement().get(i) > tmpmax){
							tmpmax = viterbi.lastElement().get(i);
							indextmp = i;
						}
					}
					if(indextmp == -1)System.out.println("bakana!!!");
					
					//TODO
					
					//uniform method : accuracy 93.86
//					for(int i=1;i<=typenum;i++){
//						tmp.add(1.0/typenum);
//						backtmp.add(indextmp);
//					}
					
					//tag distribution method : accuracy 94.31
//					for(int i=1;i<=typenum;i++){
//						tmp.add((double)(count.get(typelist.get(i)))/count.get("total"));
//						backtmp.add(indextmp);
//					}
					
					//intuitive tag distribution method : accuracy 94.68
//					for(int i=1;i<=typenum;i++){
//						if(typelist.get(i).equals("NNP")){
//							tmp.add(0.41); //referenced from book
//						}
//						else tmp.add(0.59*(double)(count.get(typelist.get(i)))/(count.get("total")-count.get("NNP")));
//						backtmp.add(indextmp);
//					}
					
					//hapax legomena method : accuracy 95.05
					for(int i=1;i<=typenum;i++){
						if(hapaxcount.get(typelist.get(i))!=null){
							tmp.add((double)(hapaxcount.get(typelist.get(i)))/hapaxcount.get("total"));
							backtmp.add(indextmp);
						}
						else{
							tmp.add(-1.0);
							backtmp.add(-1);
						}
					}
					
				}
				else{
					for(int i=1;i<=typenum;i++){
						tmp.add(-1.0);
						backtmp.add(-1);
						for(int j=0;j<=typenum;j++){
							if(viterbi.lastElement().get(j)!=-1.0){
								double prob = viterbi.lastElement().get(j);
								String formerState = typelist.get(j);
								String curState = typelist.get(i);
								if(arc.get(formerState).containsKey(curState)){
									prob  = prob * arc.get(formerState).get(curState)/count.get(formerState);
									if(emit.get(curState).containsKey(s)){
										prob = prob * emit.get(curState).get(s)/count.get(curState);
									}
									else prob = -1.0;
								}
								else prob = -1.0;
								if(prob > tmp.lastElement()){
									tmp.set(i,prob);
									backtmp.set(i, j);
								}
							}
						}
					}
					
					//check arc problem
					boolean noarcflag = true;
					for(int i=1;i<=typenum;i++){
						if(tmp.get(i) > -0.5){
							noarcflag = false;
							break;
						}
					}
					if(noarcflag){
						//set backtrack
						double tmpmax = -1.0;
						int indextmp = -1;
						for(int i=0;i<=typenum;i++){
							if(viterbi.lastElement().get(i)!=-1.0 && viterbi.lastElement().get(i) > tmpmax){
								tmpmax = viterbi.lastElement().get(i);
								indextmp = i;
							}
						}

						for(int i=1;i<=typenum;i++){
							String curState = typelist.get(i);
							if(emit.get(curState).containsKey(s)){
								tmp.set(i, (double)(emit.get(curState).get(s))/count.get(curState));
								backtmp.set(i, indextmp);
							}
						}
					}
				}
				
				tmp.add(-1.0);
				backtmp.add(-1);
				
				//normalize
				double tmpmin = 1.0;
				for(int i=1;i<=typenum;i++){
					if(tmp.get(i)!=-1.0){
						tmpmin = Math.min(tmpmin,tmp.get(i));
					}
				}
				for(int i=1;i<=typenum;i++){
					if(tmp.get(i)!=-1.0){
						tmp.set(i,tmp.get(i)/tmpmin);
					}
				}
				
				viterbi.add(tmp);
				backtrack.add(backtmp);
			}
		}
		
		brvalid.close();
		ps.close();
		
		//validation
//		PrintStream out = new PrintStream(new FileOutputStream("D:\\valid.out"));  
//        System.setOut(out); 
//		Score.validation("D:\\WSJ_24.pos","D:\\WSJ_out.txt");
	}
}
