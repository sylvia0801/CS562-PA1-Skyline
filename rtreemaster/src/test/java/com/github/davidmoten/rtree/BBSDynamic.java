package com.github.davidmoten.rtree;




import java.io.IOException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Point;


import algs.bbs.*;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.StringObservable;




public class BBSDynamic {
    
    
    
    public static Observable<Entry<Object, Point>> entries(final Precision precision) {
        Observable<String> source = Observable.using(new Func0<InputStream>() {
            @Override
            public InputStream call() {
                try {
                    return new GZIPInputStream(BBSDynamic.class
                                               .getResourceAsStream("/greek-earthquakes-1964-2000.txt.gz"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new Func1<InputStream, Observable<String>>() {
            @Override
            public Observable<String> call(InputStream is) {
                return StringObservable.from(new InputStreamReader(is));
            }
        }, new Action1<InputStream>() {
            @Override
            public void call(InputStream is) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return StringObservable.split(source, "\n")
        .flatMap(new Func1<String, Observable<Entry<Object, Point>>>() {
            
            @Override
            public Observable<Entry<Object, Point>> call(String line) {
                if (line.trim().length() > 0) {
                    String[] items = line.split(" ");
                    double lat = Double.parseDouble(items[0]);
                    double lon = Double.parseDouble(items[1]);
                    Entry<Object, Point> entry;
                    if (precision == Precision.DOUBLE)
                        entry = Entries.entry(new Object(), Geometries.point(lat, lon));
                    else
                        entry = Entries.entry(new Object(),
                                              Geometries.point((double) lat, (double) lon));
                    return Observable.just(entry);
                } else
                    return Observable.empty();
            }
        });
    }
    
    static List<Entry<Object, Point>> entriesList(Precision precision) {
        List<Entry<Object, Point>> result = entries(precision).toList().toBlocking().single();
        System.out.println("loaded greek earthquakes into list");
        return result;
    }
    
    public static void main(String[] args) throws InterruptedException {
        RTree<Object, Point> tree = RTree.star().create();
        tree = tree.add(entries(Precision.SINGLE)).last().toBlocking().single();
        System.gc();
        
        //System.out.println(tree.size());
        BBS bbs = new BBS(tree);
      /** Observable<Entry<Object, Point>> results =
        	    tree.search(Geometries.rectangle(0,0,35,40));
        for(Entry<Object, Point> entry: results ) {
            System.out.println(entry.geometry());
        }
     */   
        List<Entry<Object, Point>> skylinePoints = bbs.execute();
        System.out.println("The skyline points are:");
        for(Entry<Object, Point> entry: skylinePoints ) {
            System.out.println(entry.geometry());
        }
       
        while(true) {
            System.out.println("Please make a change to the R-tree:");
            System.out.println("Please choose below entering the number before the specific option (1/2)");
            System.out.println("1.Insert a point");
            System.out.println("2.Delete a point");
            Scanner s = new Scanner(System.in);
            String input=s.nextLine();
            
            if(input.equals("1")) {
                System.out.println("Please input x value of the point:");
                Scanner a = new Scanner(System.in);
                Double x=a.nextDouble();
                System.out.println("Please input y value of the point:");
                Scanner b = new Scanner(System.in);
                Double y=b.nextDouble();
                tree = tree.add(new Object(), Geometries.point(x,y));
                BBS bbs1 = new BBS(tree);
                List<Entry<Object, Point>> skylinePoints1 = bbs1.execute();
                System.out.println("The new skyline points are:");
                for(Entry<Object, Point> entry: skylinePoints1 ) {
                    System.out.println(entry.geometry());
                }
            }
            else if(input.equals("2")) {
                System.out.println("Please input x value of the point:");
                Scanner a = new Scanner(System.in);
                Double x=a.nextDouble();
                System.out.println("Please input y value of the point:");
                Scanner b = new Scanner(System.in);
                Double y=b.nextDouble();
                Iterable<Entry<Object, Point>> it = tree.search(Geometries.point(x,y))
                        .toBlocking().toIterable();
                
                tree = tree.delete(it,true);
                
                BBS bbs2 = new BBS(tree);
                List<Entry<Object, Point>> skylinePoints2 = bbs2.execute();
                System.out.println("The new skyline points are:");
                for(Entry<Object, Point> entry: skylinePoints2 ) {
                    System.out.println(entry.geometry());
                }
            }
            else {
            	System.out.println("!!!Please enter the correct number of choice!!!");
            	System.out.println("-----------------------------------------------");
            }
        }
    }
}
