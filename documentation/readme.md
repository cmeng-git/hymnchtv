### Content Copyrights
The project contains copyright content materials, to avoid infringement of the authors' rights;<br/>
most of the materials are not available online. Only a sample of the items is available for reference.<br/>

If you need to build android hymnchtv.apks for distribution, please consult the original contents' owner for advice:<br/>
台湾福音书房 <br/>
联络电子邮件地址: [android.tgbr@gmail.com](mailto:android.tgbr@gmail.com)</br>

### Hymnchtv content directory structures
The information described herein is valid for hymnchtv v1.2.0 and all future releases.<br/>
You may refer to the files in this directory to understand the format/filename used in each hymn category.<br/>
Except for the *.mid files, all the apk resources are now moved to assets sub-directories.<br/>
All the files are now tagged with hymn number instead of android resource running index.

* Image file for desktop background:<br/>
b.java

* 大本诗歌 lyrics contents:<br/>
Note: The traditional Chinese hymn lyrics text contains only partial hymn titles.<br/>They are not used currently for context searching nor display<br/>Sub-directories: lyrics_db_score, lyrics_db_text, lyrics_dbs_text

* 补充本 lyrics contents:<br/>
Sub-directories: lyrics_bb_score, lyrics_bb_text, lyrics_bbs_text

* 新歌颂咏 lyrics contents:<br/>
Sub-directories: lyrics_xb_score, lyrics_xb_text

* 儿童诗歌 lyrics contents:<br/>
Sub-directories: lyrics_er_score, lyrics_er_text

* 大本诗歌 midi files (main and accompany) to be played simultaneously:<br/>
raw: mdb.java & mdbc.java

* 补充本 midi files to be played simultaneously:<br/>
raw: mbb.java & mbbc.java
