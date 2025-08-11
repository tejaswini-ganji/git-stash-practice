import java.util.*;
import java.util.stream.*;
public class streams{
    public static void main(String[] args) {
        List<Integer> num=new ArrayList<>(Arrays.asList(20, 4, 5, 79,0,1,6, 7, 8, 9, 10));
        List<Integer> numStream1=num.stream().filter(n-> n%2==0).sorted().map(n->n*2).collect(Collectors.toList());
        System.out.println("Sorted List: " + numStream1);

        int sum=num.stream().reduce(0, (a, b) -> a + b);
        System.out.println("Sum of all numbers: " + sum);

        Stream<Integer> numStream2=num.stream().filter(n -> n>2);
        Stream<Integer> limtedStream = numStream2.limit(5);
        Stream<Integer> skipped= limtedStream.skip(2);
        skipped.forEach(n -> System.out.print(n + " "));

        System.out.println();
        Optional<Integer> min =num.stream().min((a,b)->a.compareTo(b));
        System.out.println(min);
        
        // System.out.println(numStream2.count() + " numbers greater than 5");
        // numStream2.forEach(n -> System.out.print(n + " "));

        // numStream2.forEach(n -> System.out.print(n + " "));

        List<String> strNums = List.of("10", "20", "30");
        System.out.println();
        List<Integer> intNums = strNums.stream()
            .map(n -> Integer.parseInt(n))
            .collect(Collectors.toList());

        System.out.println(intNums);  

        

        
    }
}