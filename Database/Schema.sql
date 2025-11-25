

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE Function [dbo].[FnEncodeStringForXML]
	(@rawString varchar(max))
	returns varchar(max)
AS
BEGIN
	return replace(replace(replace(replace(replace(@rawstring, '&', '&amp;'), '"', '&quot;'), '>', '&gt;'), '<', '&lt;'), '''', '&apos;')
END
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[qepMessageLog](
	[messageID] [int] IDENTITY(1,1) NOT NULL,
	[httpStatus] [int] NULL,
	[deliverytAttemptID] [bigint] NULL,
	[duplicate] [bit] NULL,
	[queryID] [int] NULL,
	[receiveDate] [datetime] NULL,
	[receiveDataRaw] [varchar](max) NULL,
	[error] [bit] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[qepMessageResultsLog](
	[messageID] [int] NULL,
	[deliveryAttemptID] [varchar](100) NULL,
	[queryId] [int] NULL,
	[resultType] [varchar](100) NULL,
	[setSize] [int] NULL,
	[status] [varchar](20) NULL,
	[x] [nvarchar](max) NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SHRINE_CONFIGURATION](
	[key] [varchar](55) NOT NULL,
	[value] [varchar](255) NOT NULL
) ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SHRINE_Debug_Log](
	[LogID] [int] IDENTITY(1,1) NOT NULL,
	[Query_Instance_ID] [int] NULL,
	[Date] [datetime] NULL,
	[ProcedureName] [varchar](100) NULL,
	[LocationID] [int] NULL,
	[Message] [varchar](max) NULL,
PRIMARY KEY CLUSTERED 
(
	[LogID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SHRINE_HUB_STATUSES](
	[InternalStatus] [varchar](32) NOT NULL,
	[UIStatus] [varchar](32) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[InternalStatus] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SHRINE_Node_Breakdown](
	[BreakdownID] [int] IDENTITY(1,1) NOT NULL,
	[Query_Instance_ID] [int] NULL,
	[AdapterNodeID] [bigint] NULL,
	[SHRINE_Breakdown_Type] [varchar](100) NULL,
	[i2b2_Breakdown_Type] [varchar](100) NULL,
	[col] [varchar](max) NULL,
	[val] [int] NULL,
	[lowLimit] [int] NULL,
	[tolerance] [int] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SHRINE_Node_Result](
	[Query_Instance_ID] [int] NOT NULL,
	[AdapterNodeID] [bigint] NOT NULL,
	[AdapterNodeName] [varchar](max) NULL,
	[AdapterStatus] [varchar](50) NULL,
	[AdapterStatusMessage] [varchar](max) NULL,
	[crcQueryInstanceID] [int] NULL,
	[count] [int] NULL,
	[binsize] [int] NULL,
	[stdDev] [float] NULL,
	[noiseClamp] [int] NULL,
	[lowLimit] [int] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SHRINE_Node_Result_log](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[date] [datetime] NULL,
	[Query_Instance_ID] [int] NOT NULL,
	[AdapterNodeID] [bigint] NOT NULL,
	[AdapterNodeName] [varchar](max) NULL,
	[AdapterStatus] [varchar](50) NULL,
	[AdapterStatusMessage] [varchar](max) NULL,
	[crcQueryInstanceID] [int] NULL,
	[count] [int] NULL,
	[binsize] [int] NULL,
	[stdDev] [float] NULL,
	[noiseClamp] [int] NULL,
	[lowLimit] [int] NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SHRINE_NODE_STATUSES](
	[InternalStatus] [varchar](32) NOT NULL,
	[UIStatus] [varchar](32) NOT NULL,
	[ProcessingCompleted] [bit] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[InternalStatus] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SHRINE_QUERIES](
	[QueryMasterID] [int] NOT NULL,
	[startDate] [datetime] NULL,
	[QueryDefinition] [nvarchar](max) NULL,
PRIMARY KEY CLUSTERED 
(
	[QueryMasterID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SHRINE_Query_Result](
	[Query_Instance_ID] [int] NOT NULL,
	[Status] [varchar](100) NULL,
PRIMARY KEY CLUSTERED 
(
	[Query_Instance_ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SHRINE_Query_Result_log](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[date] [datetime] NULL,
	[Query_Instance_ID] [int] NULL,
	[Status] [varchar](100) NULL
) ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SHRINE_Query_Result_Type_Mapping](
	[i2b2_type] [varchar](100) NOT NULL,
	[SHRINE_type] [varchar](100) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[i2b2_type] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE PROCEDURE [dbo].[getCRCResponseMessage]
	@queryID int,
	@previousMessageID int
AS
BEGIN
	declare @messageID int
	select @messageID = max(messageID) from [dbo].[qepMessageResultsLog] where messageID > @previousMessageID and queryId = @queryID
	--select @messageID

	if @messageID is null
	begin
		select cast(null as int) as messageID, cast(null as nvarchar(max)) x 
	end	
	else
	begin
		select @messageID as messageID, (select @queryID as "message_body/query_id", @messageID as "message_body/message_id", 
			(select resultType as "result/result_type", setSize as "result/set_size", status as "result/status", replace(replace(replace(x, '&', '&amp;'), '<', '&lt;'), '>', '&gt;') as "result/xml_value"
				from [dbo].[qepMessageResultsLog] where messageID = @messageID 
				for XML Path (''), type) "message_body/results" 
				for xml path('')) as x
	end
END
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE PROCEDURE [dbo].[newQepReceivedMessage]
	@httpStatus int = null,
	@message varchar(max)
AS
BEGIN
	declare @deliveryAttemptId bigint
	set @deliveryAttemptId = -1
	declare @messageID bigint, @queryId int
	declare @results table (deliveryAttemptID varchar(100), queryId int, resultType varchar(100), setSize int, status varchar(20), x nvarchar(max))


	declare @log bit 
	set @log = 1

	if '' = @message
	BEGIN
		set @deliveryAttemptId = -2 --This lets us identify to the Java code that this was an empty message not an error
		insert into [qepMessageLog] (receiveDate, receiveDataRaw, httpStatus, error)
		select getdate(), @message, @httpStatus, 0
		insert into @results (deliveryAttemptID) values ('empty')
		select 0 as messageID, * from @results
		return
	END
	 
	-- Immediate return put in to simplify fake hub code.
	if '{"deliveryAttemptId":"-1"}' = @message
	BEGIN
		select 0 as messageID, * from @results
		return
	END

	if @log = 1
	BEGIN
		declare @idTable TABLE ( id int)
		insert into [qepMessageLog] (receiveDate, receiveDataRaw, httpStatus )
		output Inserted.messageID into @idTable
		select getdate(), @message, @httpStatus
		select @messageID = id from @idTable
	END 

	--WAITFOR DELAY '00:00:05'


	if '' = @message
	BEGIN
		set @deliveryAttemptId = -2 --This lets us identify to the Java code that this was an empty message not an error
		set @log = 1 --modify this to prevent logging of empty messages.
	END
	BEGIN TRY
		create table #tmp1 ( k varchar(max), v varchar(max), t int) 
		create table #tmp2 ( k varchar(max), v varchar(max), t int) 
		insert into #tmp1 select * from openJson(@message)
		--select * from #tmp1
		declare @s varchar(max)
		select @s = v from #tmp1 where k = 'deliveryAttemptId'
		insert into #tmp2 select * from openJson(@s)
		select @deliveryAttemptId = v from #tmp2 where k =  'underlying'
		declare @duplicate bit
		select @duplicate = case when exists (select 1 from [qepMessageLog] where deliverytAttemptID = @deliveryAttemptId) then 1 else 0 end 
		select @s = v from #tmp1 where k = 'contents'
		--update [qepMessageLog] set deliverytAttemptID = @deliveryAttemptId, duplicate = @duplicate, contents = @s where messageID = @messageID
		if @log = 1 update [qepMessageLog] set deliverytAttemptID = @deliveryAttemptId, duplicate = @duplicate where messageID = @messageID

		truncate table #tmp2
		insert into #tmp2 select * from openJson(@s)
		declare @contentsType varchar(max)
		select @contentsType = v from #tmp2 where k = 'contentsType'
		select @queryId = v from #tmp2 where k = 'contentsSubject'
		select @s  = v from #tmp2 where k = 'contents'

		truncate table #tmp2

		if @log = 1 update [qepMessageLog] set queryID = @queryId where messageID = @messageID

		insert into #tmp2 select * from openJson(@s)
		declare @encodedClass varchar(max)
		select @encodedClass = v from #tmp2 where k = 'encodedClass'
		if @log = 1 update [qepMessageLog] set error = 0 where messageID = @messageID
	END TRY
	BEGIN CATCH
		if @log = 1 
		BEGIN
			update [qepMessageLog] set error = 1 where messageID = @messageID
		END
		ELSE
		BEGIN
			insert into [qepMessageLog] (receiveDate, receiveDataRaw, httpStatus, error )
			select getdate(), @message, @httpStatus, 1
		END
	END CATCH
	declare @a table ( k varchar(max), v varchar(max), t int) 
	declare @b table ( k varchar(max), v varchar(max), t int) 
	declare @c table ( k varchar(max), v varchar(max), t int) 
	declare @d table ( k varchar(max), v varchar(max), t int) 

	declare @status varchar(max)
	declare @AdapterNodeID bigint, @AdapterNodeName varchar(max), @AdapterStatus varchar(50), @AdapterStatusMessage varchar(max), @crcQueryInstanceID int, @count int, @binsize int, @stdDev float, @noiseClamp int, @lowLimit int

	IF @encodedClass = 'UpdateQueryAtQepWithStatus'
	BEGIN
		--insert into @a select * from openJson((select v from #tmp2 where k = 'queryStatus'))
		--select @status = v from @a where k = 'encodedClass'
		select @status = v from #tmp2 where k = 'queryStatus'
		if @log = 1 insert into SHRINE_Query_Result_log ([date], Query_Instance_ID, status) values (getdate(), @queryId, @status)
		if exists (select 1 from SHRINE_Query_Result where Query_Instance_ID = @queryId)
			update SHRINE_Query_Result set status = @status where Query_Instance_ID = @queryId
		ELSE 
			insert into SHRINE_Query_Result (Query_Instance_ID, status) values (@queryId, @status)
	END
	ELSE IF @encodedClass = 'UpdateQueryReadyForAdapters'
	BEGIN
		insert into @a select * from openJson((select v from #tmp2 where k = 'resultProgresses'))
		declare @i int 
		select @i = max(k) from @a
		while @i >=0 
		begin
			delete from @b
			insert into @b select * from openJson((select v from @a where k = @i))
			select @AdapterNodeID = v from @b where k = 'adapterNodeId'
			select @AdapterNodeName = v from @b where k = 'adapterNodeName'
			
			--delete from @c
			--insert into @c select * from openJson((select v from @b where k = 'status'))
			--select @AdapterStatus = v from @c where k = 'encodedClass'
			select @AdapterStatus = v from @b where k = 'status'

			select @AdapterStatusMessage = v from @b where k = 'statusMessage'
			select @crcQueryInstanceID = v from @b where k = 'crcQueryInstanceId'
			if @log = 1 
				insert into SHRINE_Node_Result_log([date], Query_Instance_ID, AdapterNodeID, AdapterNodeName, AdapterStatus, AdapterStatusMessage, crcQueryInstanceID)
				values(getdate(), @queryId, @AdapterNodeID, @AdapterNodeName, @AdapterStatus, @AdapterStatusMessage, @crcQueryInstanceID)

			if exists (select 1 from SHRINE_Node_Result where Query_Instance_ID = @queryId and AdapterNodeID = @AdapterNodeID)
				update SHRINE_Node_Result set AdapterNodeName = isnull(@AdapterNodeName, AdapterNodeName), 
											  AdapterStatus =  isnull(@AdapterStatus, AdapterStatus),
											  AdapterStatusMessage = isnull(@AdapterStatusMessage, AdapterStatusMessage),
											  crcQueryInstanceID = isnull(@crcQueryInstanceID, crcQueryInstanceID)
					where Query_Instance_ID = @queryId 
						and AdapterNodeID = @AdapterNodeID
						and AdapterStatus not in (select [InternalStatus] from [dbo].[SHRINE_NODE_STATUSES] where ProcessingCompleted = 1) -- Don't update a Finished Status to an unfinished status.
			else 
				insert into SHRINE_Node_Result(Query_Instance_ID, AdapterNodeID, AdapterNodeName, AdapterStatus, AdapterStatusMessage, crcQueryInstanceID)
				values(@queryId, @AdapterNodeID, @AdapterNodeName, @AdapterStatus, @AdapterStatusMessage, @crcQueryInstanceID)
			select @i = @i - 1
		end
	END
	ELSE IF @encodedClass = 'ResultProgress'
	BEGIN
			select @AdapterNodeID = v from #tmp2 where k = 'adapterNodeId'
			select @AdapterNodeName = v from #tmp2 where k = 'adapterNodeName'
			
			--delete from @c
			--insert into @c select * from openJson((select v from #tmp2 where k = 'status'))
			--select @AdapterStatus = v from @c where k = 'encodedClass'
			select @AdapterStatus = v from #tmp2 where k = 'status'

			select @AdapterStatusMessage = v from #tmp2 where k = 'statusMessage'
			select @crcQueryInstanceID = v from #tmp2 where k = 'crcQueryInstanceId'
			if @log = 1 
				insert into SHRINE_Node_Result_log([date], Query_Instance_ID, AdapterNodeID, AdapterNodeName, AdapterStatus, AdapterStatusMessage, crcQueryInstanceID)
				values(getdate(), @queryId, @AdapterNodeID, @AdapterNodeName, @AdapterStatus, @AdapterStatusMessage, @crcQueryInstanceID)

			if exists (select 1 from SHRINE_Node_Result where Query_Instance_ID = @queryId and AdapterNodeID = @AdapterNodeID)
				update SHRINE_Node_Result set AdapterNodeName = isnull(@AdapterNodeName, AdapterNodeName), 
											  AdapterStatus =  isnull(@AdapterStatus, AdapterStatus),
											  AdapterStatusMessage = isnull(@AdapterStatusMessage, AdapterStatusMessage),
											  crcQueryInstanceID = isnull(@crcQueryInstanceID, crcQueryInstanceID)
					where Query_Instance_ID = @queryId 
						and AdapterNodeID = @AdapterNodeID
						and AdapterStatus not in (select [InternalStatus] from [dbo].[SHRINE_NODE_STATUSES] where ProcessingCompleted = 1) -- Don't update a Finished Status to an unfinished status.
			else 
				insert into SHRINE_Node_Result(Query_Instance_ID, AdapterNodeID, AdapterNodeName, AdapterStatus, AdapterStatusMessage, crcQueryInstanceID)
				values(@queryId, @AdapterNodeID, @AdapterNodeName, @AdapterStatus, @AdapterStatusMessage, @crcQueryInstanceID)
			select @i = @i - 1
	END
	ELSE IF @encodedClass = 'UpdateResultWithError'
	BEGIN
			delete from @c
			insert into @c select * from openJson((select v from #tmp2 where k = 'result'))
			
			select @AdapterNodeID = v from @c where k = 'adapterNodeId'
			select @AdapterNodeName = v from @c where k = 'adapterNodeName'
			

			select @AdapterStatus = v from @c where k = 'status'

			select @AdapterStatusMessage = v from @c where k = 'problemDigest'
			select @crcQueryInstanceID = v from @c where k = 'crcQueryInstanceId'
			if @log = 1 
				insert into SHRINE_Node_Result_log([date], Query_Instance_ID, AdapterNodeID, AdapterNodeName, AdapterStatus, AdapterStatusMessage, crcQueryInstanceID)
				values(getdate(), @queryId, @AdapterNodeID, @AdapterNodeName, @AdapterStatus, @AdapterStatusMessage, @crcQueryInstanceID)

			if exists (select 1 from SHRINE_Node_Result where Query_Instance_ID = @queryId and AdapterNodeID = @AdapterNodeID)
				update SHRINE_Node_Result set AdapterNodeName = isnull(@AdapterNodeName, AdapterNodeName), 
											  AdapterStatus =  isnull(@AdapterStatus, AdapterStatus),
											  AdapterStatusMessage = isnull(@AdapterStatusMessage, AdapterStatusMessage),
											  crcQueryInstanceID = isnull(@crcQueryInstanceID, crcQueryInstanceID)
					where Query_Instance_ID = @queryId and AdapterNodeID = @AdapterNodeID
			else 
				insert into SHRINE_Node_Result(Query_Instance_ID, AdapterNodeID, AdapterNodeName, AdapterStatus, AdapterStatusMessage, crcQueryInstanceID)
				values(@queryId, @AdapterNodeID, @AdapterNodeName, @AdapterStatus, @AdapterStatusMessage, @crcQueryInstanceID)
			select @i = @i - 1
	END
	ELSE IF @encodedClass = 'UpdateResultWithProgress'
	BEGIN
			delete from @c
			insert into @c select * from openJson((select v from #tmp2 where k = 'result'))
			
			select @AdapterNodeID = v from @c where k = 'adapterNodeId'
			select @AdapterNodeName = v from @c where k = 'adapterNodeName'
			

			select @AdapterStatus = v from @c where k = 'status'

			select @AdapterStatusMessage = v from @c where k = 'statusMessage'
			select @crcQueryInstanceID = v from @c where k = 'crcQueryInstanceId'
			if @log = 1 
				insert into SHRINE_Node_Result_log([date], Query_Instance_ID, AdapterNodeID, AdapterNodeName, AdapterStatus, AdapterStatusMessage, crcQueryInstanceID)
				values(getdate(), @queryId, @AdapterNodeID, @AdapterNodeName, @AdapterStatus, @AdapterStatusMessage, @crcQueryInstanceID)

			if exists (select 1 from SHRINE_Node_Result where Query_Instance_ID = @queryId and AdapterNodeID = @AdapterNodeID)
				update SHRINE_Node_Result set AdapterNodeName = isnull(@AdapterNodeName, AdapterNodeName), 
											  AdapterStatus =  isnull(@AdapterStatus, AdapterStatus),
											  AdapterStatusMessage = isnull(@AdapterStatusMessage, AdapterStatusMessage),
											  crcQueryInstanceID = isnull(@crcQueryInstanceID, crcQueryInstanceID)
					where Query_Instance_ID = @queryId 
						and AdapterNodeID = @AdapterNodeID
						and AdapterStatus not in (select [InternalStatus] from [dbo].[SHRINE_NODE_STATUSES] where ProcessingCompleted = 1) -- Don't update a Finished Status to an unfinished status.
			else 
				insert into SHRINE_Node_Result(Query_Instance_ID, AdapterNodeID, AdapterNodeName, AdapterStatus, AdapterStatusMessage, crcQueryInstanceID)
				values(@queryId, @AdapterNodeID, @AdapterNodeName, @AdapterStatus, @AdapterStatusMessage, @crcQueryInstanceID)
			select @i = @i - 1
	END
	ELSE IF @encodedClass = 'UpdateResultWithCount'
	BEGIN
			delete from @b
			insert into @b select * from openJson((select v from #tmp2 where k = 'result'))
			select @AdapterNodeID = v from @b where k = 'adapterNodeId'
			select @AdapterNodeName = v from @b where k = 'adapterNodeName'
			
			select @AdapterStatus = v from @b where k = 'status'

			select @count = v from @b where k = 'count'
			select @count = 0 where @count = -1

			delete from @c
			insert into @c select * from openJson((select v from @b where k = 'obfuscatingParameters'))

			select @binsize = v from @c where k = 'binSize' 
			select @stdDev = v from @c where k = 'stdDev'
			select @noiseClamp = v from @c where k = 'noiseClamp'
			select @lowLimit = v from @c where k = 'lowLimit'

			delete from @c
			insert into @c select * from openJson((select v from @b where k = 'breakdowns'))

			declare @counts varchar(max)
			select @counts = v from @c where k = 'counts'

			delete from @c
			insert into @c select * from openJson(@counts)

			--declare @d table ( k varchar(max), v varchar(max), t int) 
			declare @breakdowns table (n1 int, n2 int, SHRINE_type varchar(100), j varchar(max), col varchar(max), val int)

			insert into @d
			select k, value as v, type as t from @c cross apply openJson(v)
			--delete from @d where  t <> 4
			insert into @breakdowns (n1, n2, j)
			select cast (k as int), cast ([key] as int), [value] from @d cross apply openJson(v) where t = 4
			update a set a.SHRINE_type = b.v from @breakdowns a join @d b on a.n1 = cast(b.k as int) and b.t = 1

			declare @e table (n1 int, n2 int, k int, v varchar(max))
			insert into @e select n1, n2, cast([key] as int) k, value v from @breakdowns cross apply openJson(j)
			update a set a.col = e1.v, a.val = e2.v from @breakdowns a join @e e1 on a.n1 = e1.n1 and a.n2 = e1.n2 and e1.k = 0 join @e e2 on a.n1 = e2.n1 and a.n2 = e2.n2 and e2.k = 1

			update @breakdowns set col = [dbo].[FnEncodeStringForXML](col)
			update @breakdowns set val = 0 where val = -1

			delete from SHRINE_Node_Breakdown where Query_Instance_ID = @queryId and AdapterNodeID = @AdapterNodeID

			insert into SHRINE_Node_Breakdown (Query_Instance_ID, AdapterNodeID, SHRINE_Breakdown_Type, i2b2_Breakdown_Type, col, val, lowLimit, tolerance) 
			select @queryId, @AdapterNodeID, a.SHRINE_Type, i2b2_Type, col, val, @lowLimit, 3 from @breakdowns a join [dbo].[SHRINE_Query_Result_Type_Mapping] b on a.SHRINE_type = b.SHRINE_type


			select @AdapterStatusMessage = v from @b where k = 'statusMessage'
			select @crcQueryInstanceID = v from @b where k = 'crcQueryInstanceId'
			if @log = 1 
				insert into SHRINE_Node_Result_log([date], Query_Instance_ID, AdapterNodeID, AdapterNodeName, AdapterStatus, AdapterStatusMessage, crcQueryInstanceID, [count], binsize, stdDev, noiseClamp, lowLimit)
				values(getdate(), @queryId, @AdapterNodeID, @AdapterNodeName, @AdapterStatus, @AdapterStatusMessage, @crcQueryInstanceID, @count, @binsize, @stdDev, @noiseClamp, @lowLimit)

			if exists (select 1 from SHRINE_Node_Result where Query_Instance_ID = @queryId and AdapterNodeID = @AdapterNodeID)
				update SHRINE_Node_Result set AdapterNodeName = isnull(@AdapterNodeName, AdapterNodeName), 
											  AdapterStatus =  isnull(@AdapterStatus, AdapterStatus),
											  AdapterStatusMessage = isnull(@AdapterStatusMessage, AdapterStatusMessage),
											  crcQueryInstanceID = isnull(@crcQueryInstanceID, crcQueryInstanceID),
											  [count] = isnull(@count, [count]),
											  binsize = isnull(@binsize, binsize),
											  stdDev = isnull(@stdDev, stdDev),
											  noiseClamp = isnull(@noiseClamp, noiseClamp),
											  lowLimit = isnull(@lowLimit, lowLimit)
					where Query_Instance_ID = @queryId and AdapterNodeID = @AdapterNodeID
			else 
				insert into SHRINE_Node_Result(Query_Instance_ID, AdapterNodeID, AdapterNodeName, AdapterStatus, AdapterStatusMessage, crcQueryInstanceID, [count], binsize, stdDev, noiseClamp, lowLimit)
				values(@queryId, @AdapterNodeID, @AdapterNodeName, @AdapterStatus, @AdapterStatusMessage, @crcQueryInstanceID, @count, @binsize, @stdDev, @noiseClamp, @lowLimit)
			select @i = @i - 1
	END
	ELSE
	BEGIN
		insert into SHRINE_Debug_Log (Query_Instance_ID, [Date], ProcedureName, LocationID, Message) Values (@queryId, getdate(), '[dbo].[newQepReceivedMessage]', 1, 'Unknown encoded class (' + isnull(@encodedClass, 'null') + ') in message ' + cast(@messageID as varchar(50)) + '    ------------    ' + @message)
	END

	declare @totalCount int, @completed bit = 0, @hubStatus varchar(50), @siteCount int, @siteCompleteCount int, @siteErrorCount int
	--@completed bit = 0, @hubStatus varchar(50), @siteCount int, @siteCompleteCount int, @siteErrorCount int
	select @totalCount = sum(case when [count] < lowlimit then (lowlimit + 1) / 2 else [count] end), 
			@noiseClamp = sum(case when [count] < lowlimit then (lowlimit + 1) / 2 else (noiseclamp + 1) / 2 end),
			@lowLimit = sum(lowlimit)
			from SHRINE_Node_Result where Query_Instance_ID = @QueryID

	if @totalCount <= @noiseClamp set @totalCount = 0
	if @totalCount > @noiseClamp and @totalCount <= @lowLimit set @lowLimit = 0

	select @siteCount = count(*) from SHRINE_Node_Result where Query_Instance_ID = @QueryID

	select @SiteCompleteCount = count(*) from SHRINE_Node_Result where AdapterStatus = 'ResultFromCRC' and Query_Instance_ID = @QueryID

	select @siteErrorCount = count(*) from SHRINE_Node_Result a 
		join SHRINE_NODE_STATUSES b on a.AdapterStatus = b.InternalStatus
		and Query_Instance_ID = @QueryID and ProcessingCompleted = 1 and AdapterStatus <> 'ResultFromCRC'

	select @completed = case when @siteCount > 0 AND @SiteCompleteCount + @siteErrorCount = @siteCount then 1 else 0 end

	if @completed = 1 
		select @hubStatus = case when @siteErrorCount > 0 then 'Completed with Errors' else 'Completed' end
	else 
		select @hubStatus = isnull(UIStatus, 'Unknown Status') from SHRINE_Query_Result a left join SHRINE_HUB_STATUSES b on a.Status = b.internalstatus and a.Query_Instance_ID = @queryId
	
	declare @incomplete int
	select @incomplete = case when @completed = 0 and @SiteCompleteCount > 0 then 1 else 0 end
	
	insert into @results values (cast(@deliveryAttemptId as varchar(100)), @queryID, 'PATIENT_COUNT_SHRINE_XML', @totalCount, case when @incomplete = 1 then 'INCOMPLETE' when @completed = 0 then 'PROCESSING' when @completed = 1 and @siteErrorCount > 0 then 'ERROR' else 'FINISHED' end,
								'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><ns10:i2b2_result_envelope xmlns:ns10="http://www.i2b2.org/xsd/hive/msg/result/1.1/"><body><ns10:result name="PATIENT_COUNT_SHRINE_XML"><data column="patient_count" type="int">' + cast(@totalCount as varchar(100)) + '</data> </ns10:result><SHRINE sites="' + cast(@siteCount as varchar(100)) + '" complete="' + cast(@SiteCompleteCount as varchar(100)) + '" error="' + cast(@siteErrorCount as varchar(100)) + '" status="'+ cast(@hubStatus as varchar(100)) +'" floorThresholdNumber="' + cast(@lowLimit as varchar(100)) + '" obfuscatedDisplayNumber="' + cast(@noiseClamp as varchar(100)) + '" /></body></ns10:i2b2_result_envelope>')
	insert into @results select cast(@deliveryAttemptId as varchar(100)), @queryID, 'PATIENT_SITE_COUNT_SHRINE_XML', @totalCount, case when @incomplete = 1 then 'INCOMPLETE' when @completed = 0 then 'PROCESSING' when @completed = 1 and @siteErrorCount > 0 then 'ERROR' else 'FINISHED' end,
									'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><ns10:i2b2_result_envelope><body><ns10:result name="PATIENT_SITE_COUNT_SHRINE_XML">' 
									+ STRING_AGG('<data column="' + [dbo].[FnEncodeStringForXML](AdapterNodeName) + '" type="' + case when AdapterStatus = 'ResultFromCRC' then 'int">' + cast([count] as varchar(50)) else 'string">' + isnull(UIStatus, 'Unknown Status') end + '</data>', '') 
									+ '</ns10:result><SHRINE sites="' + cast(@siteCount as varchar(100)) + '" complete="' + cast(@SiteCompleteCount as varchar(100)) + '" error="' + cast(@siteErrorCount as varchar(100)) + '" status="'+ cast(@hubStatus as varchar(100)) +'" >'
									+ STRING_AGG('<site name="' + [dbo].[FnEncodeStringForXML](AdapterNodeName) + '" status="' + isnull(UIStatus, 'Unknown Status') + '"'  + case when AdapterStatus = 'ResultFromCRC' then ' floorThresholdNumber="' + cast(lowLimit as varchar(100)) + '" obfuscatedDisplayNumber="' + cast((noiseclamp + 1) / 2 as varchar(100)) + '" binSize="0" stdDev="3" />' else '>' + isnull([dbo].[FnEncodeStringForXML](AdapterStatusMessage), 'No Error Message') + '</site>' end, '') 
									+ '</SHRINE></body></ns10:i2b2_result_envelope>'
			from SHRINE_Node_Result a left join SHRINE_NODE_STATUSES b on a.AdapterStatus = b.InternalStatus where a.Query_Instance_ID = @queryId

	insert into @results select distinct cast(@deliveryAttemptId as varchar(100)), @queryID, i2b2_Breakdown_Type, @totalCount, case when @incomplete = 1 then 'INCOMPLETE' when @completed = 0 then 'PROCESSING' when @completed = 1 and @siteErrorCount > 0 then 'ERROR' else 'FINISHED' end, null
	from SHRINE_Node_Breakdown where Query_Instance_ID = @queryId

	declare @breakdownSummary table (i2b2_Breakdown_Type varchar(100), col varchar(100), val1 int, val2 int, lowlimit int, tolerance int)
	insert into @breakdownSummary select i2b2_Breakdown_Type, col, sum(val), sum(case when val = 0 then (lowLimit + 1) / 2 else val end), sum(lowLimit), sum (case when val = 0 then (lowLimit + 1) / 2 else tolerance end)  from SHRINE_Node_Breakdown where Query_Instance_ID = @queryId group by i2b2_Breakdown_Type, col 


	update @results set x = 
		'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><ns10:i2b2_result_envelope><body>' + 
		replace((select resultType as "ns10result/@name",
			(select col "data/@column", lowLimit "data/@floorThresholdNumber", tolerance "data/@obfuscatedDisplayNumber", case when val1 = 0 then val1 else val2 end as "data"
				from @breakdownSummary d where d.i2b2_Breakdown_Type = resultType
				for XML Path (''), type) "ns10result"
				for XML Path ('')), 'ns10result', 'ns10:result')
		+
		(select @siteCount as "SHRINE/@sites", @SiteCompleteCount as "SHRINE/@complete", @siteErrorCount as "SHRINE/@error",
			(select AdapterNodeName as "site/@name", UIStatus as "site/@status", binsize as "site/@binsize", stdDev as "site/@stdDev", noiseClamp as "site/@obfuscatedDisplayNumber", lowLimit as "site/@floorThresholdNumber",(select col as "siteresult/@column", 'int' as "siteresult/@type", val as "siteresult" from SHRINE_Node_Breakdown a1 where a1.AdapterNodeID = a.AdapterNodeID and a1.Query_Instance_ID = @queryId and a1.i2b2_Breakdown_Type = resultType for xml path(''), type) "site"
				from [dbo].[SHRINE_Node_Result] a left join [dbo].[SHRINE_NODE_STATUSES] b on a.AdapterStatus = b.InternalStatus where Query_Instance_ID = @queryId
				for XML Path (''), type) "SHRINE"
				for XML Path (''))
		+ '</body></ns10:i2b2_result_envelope>'
		where x is null
	
	insert into qepMessageResultsLog select @messageID, * from @results

	select @messageID as messageID, * from @results

END
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE procedure [dbo].[SHRINE_CREATE_QUERY]
	@QueryMasterID int,
	@x varchar(max),
	@ix xml

AS
BEGIN

	insert into [dbo].[SHRINE_QUERIES] (StartDate, QueryMasterID) 
	values (getDate(), @QueryMasterID)


	declare @json varchar(max)
	set @json = '{"query":{"id":0,"versionInfo":{"protocolVersion":2,"shrineVersion":"4.4.0-SNAPSHOT","itemVersion":2,"createDate":1733258225881,"changeDate":1733258225881},"status":"SentToHub","queryDefinition":{"expression":{"xmlStr":"","encodedClass":"XmlString"}},"outputXml":"","breakdownNames":[],"queryName":"","queryNotes":"","queryFaved":false,"nodeOfOriginId":0,"researcherId":0},"researcher":{"id":0,"versionInfo":{"protocolVersion":2,"shrineVersion":"4.4.0-SNAPSHOT","itemVersion":1,"createDate":0,"changeDate":0},"userName":"","userDomainName":"","nodeId":0},"protocolVersion":2}'
	declare @nodeOfOriginId varchar(255), @projectName varchar(255), @userName varchar(255), @userDomainName varchar(255), @topicDescription varchar(255), @topicName varchar(255), @hubURL varchar(255)
	select @nodeOfOriginId = [value] from [dbo].[SHRINE_CONFIGURATION] where [key] = 'nodeOfOriginId'
	select @projectName = [value] from [dbo].[SHRINE_CONFIGURATION] where [key] = 'projectName'
	select @userName = [value] from [dbo].[SHRINE_CONFIGURATION] where [key] = 'userName'
	select @userDomainName = [value] from [dbo].[SHRINE_CONFIGURATION] where [key] = 'userDomainName'
	select @topicDescription = [value] from [dbo].[SHRINE_CONFIGURATION] where [key] = 'topicDescription'
	select @topicName = [value] from [dbo].[SHRINE_CONFIGURATION] where [key] = 'topicName'
	select @hubURL = [value] from [dbo].[SHRINE_CONFIGURATION] where [key] = 'hubURL'

	declare @resultOutputTable table (name varchar(100), priority_index int, SHRINE_type varchar(100))
	insert into @resultOutputTable (name, priority_index) select nref.value('./@name','varchar(50)'), nref.value('./@priority_index','varchar(50)') from @ix.nodes('//result_output') as R(nref)
	update a set a.SHRINE_type = b.SHRINE_type from @resultOutputTable a join SHRINE_Query_Result_Type_Mapping b on a.name = b.i2b2_type
	delete from @resultOutputTable where SHRINE_type is null
	--select *, row_number() over (order by priority_index) from @resultOutputTable

	declare @outputXML varchar(max)
	--set @queryDefinition = '<query_definition><query_name>GenderNewAgainXml@12:58:04</query_name><query_timing>ANY</query_timing><specificity_scale>0</specificity_scale><use_shrine>1</use_shrine><panel><panel_number>1</panel_number><panel_accuracy_scale>100</panel_accuracy_scale><invert>0</invert><panel_timing>ANY</panel_timing><total_item_occurrences>1</total_item_occurrences><item><hlevel>3</hlevel><item_name>Gender</item_name><item_key>\\i2b2_DEMO\i2b2\Demographics\Gender\</item_key><tooltip>\\i2b2_DEMO\i2b2\Demographics\Gender\</tooltip><class>ENC</class><constrain_by_date></constrain_by_date><item_icon>LA</item_icon><item_is_synonym>false</item_is_synonym></item></panel></query_definition>'
	--set @outputXML = '<result_output_list><result_output priority_index=''1'' name=''patient_count_xml''/><result_output priority_index=''2'' name=''patient_age_count_xml''/><result_output priority_index=''3'' name=''patient_gender_count_xml''/><result_output priority_index=''4'' name=''patient_race_count_xml''/><result_output priority_index=''5'' name=''patient_vitalstatus_count_xml''/><result_output priority_index=''6'' name=''patient_inout_xml''/></result_output_list>'
	--set @outputXML = '<result_output_list><result_output priority_index=''1'' name=''patient_count_xml''/><result_output priority_index=''2'' name=''patient_age_count_xml''/><result_output priority_index=''3'' name=''patient_gender_count_xml''/><result_output priority_index=''4'' name=''patient_race_count_xml''/><result_output priority_index=''5'' name=''patient_vitalstatus_count_xml''/></result_output_list>'
	--set @outputXML = '<result_output_list><result_output priority_index=''1'' name=''patient_count_xml''/></result_output_list>'
	set @outputXML = (select (select SHRINE_type "result_output/@name", row_number() over (order by priority_index) "result_output/@priority_index" from @resultOutputTable FOR XML PATH (''), type )for XML Path ('result_output_list'))

	set @json = JSON_MODIFY(@json, '$.query.id', @QueryMasterID)
	set @json = JSON_MODIFY(@json, '$.query.nodeOfOriginId', cast(@nodeOfOriginId as bigint))
	--set @json = JSON_MODIFY(@json, '$.query.projectName', @projectName)
	set @json = JSON_MODIFY(@json, '$.researcher.userName', @userName)
	set @json = JSON_MODIFY(@json, '$.researcher.userDomainName', @userDomainName)
	set @json = JSON_MODIFY(@json, '$.researcher.nodeId', cast(@nodeOfOriginId as bigint))
	--set @json = JSON_MODIFY(@json, '$.topic.description', @topicDescription)
	--set @json = JSON_MODIFY(@json, '$.topic.name', @topicName)
	set @json = JSON_MODIFY(@json, '$.query.queryDefinition.expression.xmlStr', @x)
	set @json = JSON_MODIFY(@json, '$.query.outputXml', @outputXML)
	
	UPDATE [dbo].[SHRINE_QUERIES] SET QueryDefinition = @json where QueryMasterID = @QueryMasterID

	declare @outerJson varchar(max)
	set @outerJson = '{"contentsType": "RunQueryAtHub","contentsSubject": 0,"contents": "","protocolVersion": 2,"shrineVersion":"4.4.0-SNAPSHOT"}'
	set @outerJson = JSON_MODIFY(@outerJson, '$.contents', @json)

	select @hubURL hub_url, @outerJson content, @QueryMasterID qep_query_id

END 

GO
