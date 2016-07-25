package com.beepcast.model.transaction;

import java.io.Serializable;
import java.util.Date;

/*******************************************************************************
 * Download Bean.
 * <p>
 * 
 * @author Alan Megargel
 * @version 1.01
 ******************************************************************************/
public class DownloadBean implements Serializable {

  private long recordCount = 0L;
  private Date firstRecordDate = new Date();
  private Date lastRecordDate = new Date();
  private long downloadFileSize = 0L;
  private String folderName = "";
  private String exeFilename = "";
  private String csvFilename = "";

  /*****************************************************************************
   * No-args constructor.
   ****************************************************************************/
  public DownloadBean() {
  }

  /*****************************************************************************
   * Set record count.
   ****************************************************************************/
  public void setRecordCount( long recordCount ) {
    this.recordCount = recordCount;
  }

  /**
   * Get record count.
   */
  public long getRecordCount() {
    return recordCount;
  }

  /*****************************************************************************
   * Set first record date.
   ****************************************************************************/
  public void setFirstRecordDate( Date firstRecordDate ) {
    this.firstRecordDate = firstRecordDate;
  }

  /**
   * Get first record date.
   */
  public Date getFirstRecordDate() {
    return firstRecordDate;
  }

  /*****************************************************************************
   * Set last record date.
   ****************************************************************************/
  public void setLastRecordDate( Date lastRecordDate ) {
    this.lastRecordDate = lastRecordDate;
  }

  /**
   * Get last record date.
   */
  public Date getLastRecordDate() {
    return lastRecordDate;
  }

  /*****************************************************************************
   * Set download file size.
   ****************************************************************************/
  public void setDownloadFileSize( long downloadFileSize ) {
    this.downloadFileSize = downloadFileSize;
  }

  /**
   * Get download file size.
   */
  public long getDownloadFileSize() {
    return downloadFileSize;
  }

  /*****************************************************************************
   * Set folder name.
   ****************************************************************************/
  public void setFolderName( String folderName ) {
    this.folderName = folderName;
  }

  /**
   * Get folder name.
   */
  public String getFolderName() {
    return folderName;
  }

  /*****************************************************************************
   * Set exe filename.
   ****************************************************************************/
  public void setExeFilename( String exeFilename ) {
    this.exeFilename = exeFilename;
  }

  /**
   * Get exe filename.
   */
  public String getExeFilename() {
    return exeFilename;
  }

  /*****************************************************************************
   * Set csv filename.
   ****************************************************************************/
  public void setCsvFilename( String csvFilename ) {
    this.csvFilename = csvFilename;
  }

  /**
   * Get csv filename.
   */
  public String getCsvFilename() {
    return csvFilename;
  }

} // eof
