<html>
<head>
<title>Strict Ordering</title>
 <link rel="stylesheet" href="resources/main.css" media="screen"/>
<script type="text/javascript" src="resources/prototype.js">
</script>

<script language="javascript">           
		var receivedUpdater = new Ajax.PeriodicalUpdater('received', 'refreshReceived',{
                method: 'get',
                frequency: 2,
                decay: 1 
            });
		var orderedUpdater = new Ajax.PeriodicalUpdater('ordered', 'refreshOrdered',{
            method: 'get',
            frequency: 2,
            decay: 1
        });
           
         receivedUpdater.start();
         orderedUpdater.start();

       function filter(el) {
           var url = "setFilter?filter="+el.value
    	   new Ajax.Request(url, {
    		   method: 'get',
    		   onSuccess: function(transport) {
    		   }
    		 });
       }  
    </script>
</head>
<body onload="filter(document.filter);">
<p>
The <i>Received</i> messages on the left show the order of the messages that arrived at the <i>strict.ordering.inbound</i> message channel. The <i>Ordered</i> side 
shows the messages as they arrive on the <i>worker.outbound</i> message channel. Note that there is a random delay of up to 3 seconds to simulate a processing load 
and to introduce some randomness to the order in which each worker completes processing. With all 5 entities shown, observe that the overall message order is not 
preserved. But if you filter by entity key, you can see strict ordering is enforced for each entity.
</p>
<br/>
<hr/>
Test0 <input type="radio" name="filter" value="Test0" onchange="filter(this)"/>
Test1<input type="radio" name="filter" value="Test1" onchange="filter(this)"/>
Test2 <input type="radio" name="filter" value="Test2" onchange="filter(this)"/>
Test3 <input type="radio" name="filter" value="Test3" onchange="filter(this)"/>
Test4 <input type="radio" name="filter" value="Test4" onchange="filter(this)"/>
All<input type="radio" name="filter" value="all" checked="checked" onchange="filter(this)"/>

  <table>
    <tr>
    	<th>Received</th>
    	<th>Ordered</th>
    </tr>
    <tr>
     <td valign="top"><div id="received"> 
        </div>
     </td>
     <td valign="top">
        <div id="ordered">
        </div>
     </td>
   
    </tr>
</table>
 
</body>
 
</html>