package hystrix_plus;

public class StackTraceUtils {
    public static boolean isSameThreadThrowable(Throwable thereThrowable, int ignoreOuterFrames) {
        assert ignoreOuterFrames >= 0;
        if (thereThrowable == null)
            return false;
        StackTraceElement[] thereStackTrace = thereThrowable.getStackTrace();
        StackTraceElement[] hereStackTrace = new Throwable().getStackTrace();
        int therePos = thereStackTrace.length;
        int herePos = hereStackTrace.length;
        while (--therePos >= 0 && --herePos >= ignoreOuterFrames) {
            if (!thereStackTrace[therePos].equals(hereStackTrace[herePos]))
                return false;
        }
        return true;
    }
}
