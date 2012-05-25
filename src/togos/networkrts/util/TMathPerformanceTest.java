package togos.networkrts.util;

public class TMathPerformanceTest
{
	public static void main( String[] args ) {
		// Attempt to get the JVM 'warmed up':
		for( int i=0; i<10000; ++i ) {
			TMath.periodic24( i );
			Math.sin( i );
		}
		
		long beginTime;
		long sinTime = 0;
		long periodicTime = 0;
		float acc = 0;
		for( int i=0; i<100; ++i ) {
			beginTime = System.currentTimeMillis();
			for( int j=0; j<1000000; ++j ) {
				TMath.periodic24( i * j );
			}
			periodicTime += System.currentTimeMillis() - beginTime;
			beginTime = System.currentTimeMillis();
			for( int j=0; j<100000; ++j ) {
				Math.sin( i * j );
			}
			sinTime += System.currentTimeMillis() - beginTime;
		}
		
		System.out.println(String.format("Sin: % 10d ms",sinTime));
		System.out.println(String.format("Per: % 10d ms",periodicTime));
		System.out.println("Accumulator: "+acc);
	}
}
