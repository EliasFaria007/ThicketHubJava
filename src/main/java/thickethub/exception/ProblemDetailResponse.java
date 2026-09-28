package thickethub.exception;

public class ProblemDetailResponse extends RuntimeException {
    public ProblemDetailResponse(String message) {
        super(message);
    }
}
