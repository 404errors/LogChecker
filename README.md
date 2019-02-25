I started this with a very simple purpose.
Our customer Alliantz has a very strange case that record in table MFG_MRP_SUM_MAT gets lost, we cannot simulate the case and have to check SQL log to investigate.
Our server generates 100 log files (10 MB/file) and I have to check for every sql command and find out the command which delete data from table MFG_MRP_SUM_MAT.
That's it.