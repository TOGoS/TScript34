;; (setq indent-tabs-mode nil)
(import (chicken irregex)
        (chicken io))

(define (run-from-port port)
  (let ((line (read-line port)))
    (if (not (eof-object? line))
        (begin
          (display (string-append "read: " line "\n"))
          (run-from-port port)
          ))))

(define (main args)
  (if (null? args)
      (display "WARNING: no test case files were indicated\n")
      (for-each
        (lambda (file)
          (if (string=? file "-")
              (run-from-port (current-input-port))
              (call-with-input-file file run-from-port)))
        args)))
