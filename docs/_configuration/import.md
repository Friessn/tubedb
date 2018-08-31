---
title: "Data Import Configuration"
---

Import of station raw source files is specified by import type and path to data files.

### import types

For file format description see [import formats](../../documentation/import_formats).

* `csv`
  
  Generic CSV-files.


* `asc`

  Generic ASC logger files.
  

* `udbf_be`

  Generic UDBF files with some "be" specific extensions.	


* `tsa`

  Generic TSA files.
  
  
* `csv_tfi`

  "tfi" specific CSV-files.	


* `asc_ki`

  "ki" specific ASC-files.

* `asc_sa_own`

  "sa_own" specific ASC-files.


### `import.ini`

Ini-file defines imports. For each region one section specifies paths to data files.

There may be multiple entries per section (possibly of same import type).

file structure:

`[REGION_NAME]`

`IMPORT_TYPE = PATH`
 
...

...

