package art.velyntora.core;
/** Conservative bounds for Android decoders that round subsampled dimensions up. */
final class ImageDecodePolicy {
 static int sampleSize(int width,int height){
  if(width<=0||height<=0)throw new IllegalArgumentException("Invalid image dimensions");
  int sample=1;
  while(true){long w=((long)width+sample-1)/sample,h=((long)height+sample-1)/sample;if(w<=8192&&h<=8192&&w*h<=4000000)return sample;if(sample>=1<<30)throw new IllegalArgumentException("Image dimensions too large");sample*=2;}
 }
}
