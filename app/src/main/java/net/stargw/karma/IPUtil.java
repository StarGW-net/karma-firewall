package net.stargw.karma;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class IPUtil {

    public static List<CIDR> toCIDR(String start, String end) throws UnknownHostException {
        return toCIDR(InetAddress.getByName(start), InetAddress.getByName(end));
    }

    public static List<CIDR> toCIDR(InetAddress start, InetAddress end) throws UnknownHostException {
        List<CIDR> listResult = new ArrayList<>();

        // Log.i(TAG, "toCIDR(" + start.getHostAddress() + "," + end.getHostAddress() + ")");

        long from = inet2long(start);
        long to = inet2long(end);
        while (to >= from) {
            byte prefix = 32;
            while (prefix > 0) {
                long mask = prefix2mask(prefix - 1);
                if ((from & mask) != from)
                    break;
                prefix--;
            }

            byte max = (byte) (32 - Math.floor(Math.log(to - from + 1) / Math.log(2)));
            if (prefix < max)
                prefix = max;

            listResult.add(new CIDR(long2inet(from), prefix));

            from += Math.pow(2, (32 - prefix));
        }

        for (CIDR cidr : listResult)
            Logs.myLog("Subnets: " + cidr.toString(), 3);


        return listResult;
    }

    private static long prefix2mask(int bits) {
        return (0xFFFFFFFF00000000L >> bits) & 0xFFFFFFFFL;
    }

    private static long inet2long(InetAddress addr) {
        long result = 0;
        if (addr != null)
            for (byte b : addr.getAddress())
                result = result << 8 | (b & 0xFF);
        return result;
    }

    private static InetAddress long2inet(long addr) {
        try {
            byte[] b = new byte[4];
            for (int i = b.length - 1; i >= 0; i--) {
                b[i] = (byte) (addr & 0xFF);
                addr = addr >> 8;
            }
            return InetAddress.getByAddress(b);
        } catch (UnknownHostException ignore) {
            return null;
        }
    }

    public static InetAddress minus1(InetAddress addr) {
        return long2inet(inet2long(addr) - 1);
    }

    public static InetAddress plus1(InetAddress addr) {
        return long2inet(inet2long(addr) + 1);
    }

    public static class CIDR implements Comparable<CIDR> {
        public InetAddress address;
        public int prefix;

        public CIDR(InetAddress address, int prefix) {
            this.address = address;
            this.prefix = prefix;
        }

        public CIDR(String ip, int prefix) {
            try {
                this.address = InetAddress.getByName(ip);
                this.prefix = prefix;
            } catch (UnknownHostException ex) {
                // Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            }
        }


        private static long ipToLong(InetAddress ip) {
            byte[] octets = ip.getAddress();
            long result = 0;
            for (byte octet : octets) {
                result <<= 8;
                result |= octet & 0xff;
            }
            return result;
        }

        public static String hextoIPString(String hexValue) {
            String ip = "";

            for (int i = hexValue.length() - 2 ; i >= 0 ; i = i - 2) {
                ip = ip + Integer.valueOf(hexValue.substring(i, i + 2), 16) + ".";
            }

            ip = ip.substring(0,ip.length()-1);
            // System.out.println("Ip = " + ip);
            Logs.myLog("Hex To IP: " + ip, 3);

            return ip;
        }

        private static InetAddress hextoIP(String hexValue) {
            String ip = "";

            for (int i = hexValue.length() - 2 ; i >= 0 ; i = i - 2) {
                ip = ip + Integer.valueOf(hexValue.substring(i, i + 2), 16) + ".";
            }

            ip = ip.substring(0,ip.length()-1);
            // System.out.println("Ip = " + ip);
            Logs.myLog("Hex To IP: " + ip, 3);

            try {
                return(InetAddress.getByName(ip));
            } catch (Exception ex) {
                // Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                Logs.myLog("Exception getting IP: " + ex, 3);
                return null;
            }
        }

        public boolean compare(String i) {

            InetAddress start = long2inet(inet2long(this.address) & prefix2mask(this.prefix));
            InetAddress stop = long2inet((inet2long(this.address) & prefix2mask(this.prefix)) + (1L << (32 - this.prefix)) - 1);

            InetAddress ip = hextoIP(i);

            if (ip == null)
            {
                return false;

            }

            if ( (ipToLong(ip) < ipToLong(start)) || (ipToLong(ip) > ipToLong(stop)) )
            {
                Logs.myLog("Not local IP", 3);
                return false;
            } else {
                Logs.myLog("Local IP", 2);
                return true;
            }

        }

        public InetAddress getStart() {
            return long2inet(inet2long(this.address) & prefix2mask(this.prefix));
        }

        public InetAddress getEnd() {
            return long2inet((inet2long(this.address) & prefix2mask(this.prefix)) + (1L << (32 - this.prefix)) - 1);
        }

        @Override
        public String toString() {
            return address.getHostAddress() + "/" + prefix + "=" + getStart().getHostAddress() + "..." + getEnd().getHostAddress();
        }

        @Override
        public int compareTo(CIDR other) {
            Long lcidr = IPUtil.inet2long(this.address);
            Long lother = IPUtil.inet2long(other.address);
            return lcidr.compareTo(lother);
        }
    }
}
