import java.util.*;
import java.util.stream.*;
public class streeeeeeeeems {
    public static void main(String[] args) {
        
    
    List<Integer> num = new ArrayList<>(Arrays.asList(20, 4, 5, 79, 0, 1, 6, 7, 8, 9, 10));
    Stream<Integer> str=num.stream()
        .filter(n -> n % 2 == 0);

        num.parallelStream()
               .filter(n -> n % 2 == 0)
               .forEach(n -> System.out.println("Thread: " + Thread.currentThread().getName() + "=> " + n));

        int[] arr = {1, 2, 3, 4, 5};
        int s = Arrays.stream(arr).sum();
        System.out.println("Sum: " + s);
    }
}
