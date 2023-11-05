package com.flyerssoft.ams.exception;

/**
 * EmployeeAlreadyExistException class.
 */
public class EmployeeAlreadyExistException extends AmsException {
  private static final long serialVersionUID = 1L;

  public EmployeeAlreadyExistException(String message) {
    super(message);
  }
}
