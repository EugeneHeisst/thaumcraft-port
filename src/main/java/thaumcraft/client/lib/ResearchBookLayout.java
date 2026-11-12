package thaumcraft.client.lib;
import java.util.*;
import java.util.regex.*;
/** Original TCFontRenderer wrapping and insert layout, independent of the rendering platform. */
public final class ResearchBookLayout {
 private final byte[] glyphWidth;
 public ResearchBookLayout(byte[] widths) { if(widths.length!=65536)throw new IllegalArgumentException("glyph metrics");glyphWidth=widths.clone(); }
 public int glyph(char c) {return glyphWidth[c]&255;}
 public int getCharWidth(char c) {if(c=='§')return -1;if(c==' ')return 4;int b=glyph(c);if(b==0)return 0;int j=b>>>4,k=b&15;if(k>7){k=15;j=0;}return (k+1-j)/2+1;}
 public int width(String text) {int width=0;boolean bold=false;for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c=='§'&&i+1<text.length()){char code=Character.toLowerCase(text.charAt(++i));if(code=='l')bold=true;else if(code=='r'||isFormatColor(code))bold=false;continue;}width+=getCharWidth(c)+(bold?1:0);}return width;}
 public enum Kind {TEXT, LINE, IMAGE}
 public record Part(Kind kind,String value,int y) {}
 public List<Part> layout(String text,int width) {
  var inserts=new ArrayList<String>();text=text.replace("<BR>","\n").replace("<BR/>","\n");
  var m=Pattern.compile("<LINE/?>|<IMG>.*?</IMG>").matcher(text);var clean=new StringBuffer();
  while(m.find()){inserts.add(m.group());m.appendReplacement(clean,"\n@"+(inserts.size()-1)+"@\n");}m.appendTail(clean);
  var result=new ArrayList<Part>();int y=0;
  for(String line:wrapFormattedStringToWidth(clean.toString(),width).split("\n")) {
   String id=line.replaceAll("§.","").trim();
   if(id.matches("@[0-9]+@")){String value=inserts.get(Integer.parseInt(id.substring(1,id.length()-1)));if(value.startsWith("<LINE"))result.add(new Part(Kind.LINE,value,y));else{result.add(new Part(Kind.IMAGE,value,y));String[] p=value.substring(5,value.length()-6).split(":");y=(int)(y+Integer.parseInt(p[5])*Float.parseFloat(p[6])-9);}}
   else result.add(new Part(Kind.TEXT,line,y));y+=9;
  }return List.copyOf(result);
 }
    String wrapFormattedStringToWidth(String par1Str, int par2) {
        int j = this.sizeStringToWidth(par1Str, par2);
        if (par1Str.length() <= j) {
            return par1Str;
        }
        String s1 = par1Str.substring(0, j);
        char c0 = par1Str.charAt(j);
        boolean flag = c0 == ' ' || c0 == '\n';
        String s2 = ResearchBookLayout.getFormatFromString(s1) + par1Str.substring(j + (flag ? 1 : 0));
        return s1 + "\n" + this.wrapFormattedStringToWidth(s2, par2);
    }

    private int sizeStringToWidth(String par1Str, int par2) {
        int l;
        int j = par1Str.length();
        int k = 0;
        int i1 = -1;
        boolean flag = false;
        for (l = 0; l < j; ++l) {
            char c0 = par1Str.charAt(l);
            switch (c0) {
                case '\u00a7': {
                    char c1;
                    if (l >= j - 1) break;
                    if ((c1 = par1Str.charAt(++l)) != 'l' && c1 != 'L') {
                        if (c1 != 'r' && c1 != 'R' && !ResearchBookLayout.isFormatColor(c1)) break;
                        flag = false;
                        break;
                    }
                    flag = true;
                    break;
                }
                case ' ': {
                    i1 = l;
                }
                default: {
                    k += this.getCharWidth(c0);
                    if (!flag) break;
                    ++k;
                }
            }
            if (c0 == '\n') {
                i1 = ++l;
                break;
            }
            if (k > par2) break;
        }
        return l != j && i1 != -1 && i1 < l ? i1 : l;
    }

    private static boolean isFormatColor(char par0) {
        return par0 >= '0' && par0 <= '9' || par0 >= 'a' && par0 <= 'f' || par0 >= 'A' && par0 <= 'F';
    }

    private static boolean isFormatSpecial(char par0) {
        return par0 >= 'k' && par0 <= 'o' || par0 >= 'K' && par0 <= 'O' || par0 == 'r' || par0 == 'R';
    }

    private static String getFormatFromString(String par0Str) {
        String s1 = "";
        int i = -1;
        int j = par0Str.length();
        while ((i = par0Str.indexOf(167, i + 1)) != -1) {
            if (i >= j - 1) continue;
            char c0 = par0Str.charAt(i + 1);
            if (ResearchBookLayout.isFormatColor(c0)) {
                s1 = "\u00a7" + c0;
                continue;
            }
            if (!ResearchBookLayout.isFormatSpecial(c0)) continue;
            s1 = s1 + "\u00a7" + c0;
        }
        return s1;
    }

}
