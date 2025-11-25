INSERT [dbo].[SHRINE_CONFIGURATION] ([key], [value]) VALUES (N'hubURL', N'https://huburl.example.com:6443/shrine-api/mom/sendMessage/examplehubname')
GO
INSERT [dbo].[SHRINE_CONFIGURATION] ([key], [value]) VALUES (N'nodeOfOriginId', N'')
GO
INSERT [dbo].[SHRINE_CONFIGURATION] ([key], [value]) VALUES (N'projectName', N'')
GO
INSERT [dbo].[SHRINE_CONFIGURATION] ([key], [value]) VALUES (N'topicDescription', N'')
GO
INSERT [dbo].[SHRINE_CONFIGURATION] ([key], [value]) VALUES (N'topicName', N'')
GO
INSERT [dbo].[SHRINE_CONFIGURATION] ([key], [value]) VALUES (N'userDomainName', N'')
GO
INSERT [dbo].[SHRINE_CONFIGURATION] ([key], [value]) VALUES (N'userName', N'')
GO
INSERT [dbo].[SHRINE_HUB_STATUSES] ([InternalStatus], [UIStatus]) VALUES (N'HubError', N'Network Error')
GO
INSERT [dbo].[SHRINE_HUB_STATUSES] ([InternalStatus], [UIStatus]) VALUES (N'IdAssigned', N'Submitted')
GO
INSERT [dbo].[SHRINE_HUB_STATUSES] ([InternalStatus], [UIStatus]) VALUES (N'QepError', N'Submission Error')
GO
INSERT [dbo].[SHRINE_HUB_STATUSES] ([InternalStatus], [UIStatus]) VALUES (N'ReadyForAdapters', N'In Progress')
GO
INSERT [dbo].[SHRINE_HUB_STATUSES] ([InternalStatus], [UIStatus]) VALUES (N'ReceivedByHub', N'Submitted')
GO
INSERT [dbo].[SHRINE_HUB_STATUSES] ([InternalStatus], [UIStatus]) VALUES (N'SentToAdapters', N'In Progress')
GO
INSERT [dbo].[SHRINE_HUB_STATUSES] ([InternalStatus], [UIStatus]) VALUES (N'SentToHub', N'Submitted')
GO
INSERT [dbo].[SHRINE_HUB_STATUSES] ([InternalStatus], [UIStatus]) VALUES (N'UnknownFinal', N'Network Error')
GO
INSERT [dbo].[SHRINE_HUB_STATUSES] ([InternalStatus], [UIStatus]) VALUES (N'UnknownInTransit', N'Submitted')
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'ErrorFromCRC', N'Site Error', 1)
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'ErrorInShrine', N'Site Error', 1)
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'IdAssigned', N'Processing at Hub', 0)
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'QueuedByAdapter', N'Delayed At Site', 0)
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'QueuedByCRC', N'Delayed At Site', 0)
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'ReadyToSubmit', N'Processing at Site', 0)
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'ReceivedByAdapter', N'Processing at Site', 0)
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'ResultFromCRC', N'Completed', 1)
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'SentToAdapter', N'Sent to Site', 0)
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'SubmittedToCRC', N'Processing at Site', 0)
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'UnknownFinal', N'Site Error', 1)
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'UnknownNotFinal', N'Delayed At Site', 0)
GO
INSERT [dbo].[SHRINE_NODE_STATUSES] ([InternalStatus], [UIStatus], [ProcessingCompleted]) VALUES (N'UnknownWhileQueuedByCRC', N'Delayed At Site', 0)
GO
INSERT [dbo].[SHRINE_Query_Result_Type_Mapping] ([i2b2_type], [SHRINE_type]) VALUES (N'PATIENT_AGE_COUNT_SHRINE_XML', N'patient_age_count_xml')
GO
INSERT [dbo].[SHRINE_Query_Result_Type_Mapping] ([i2b2_type], [SHRINE_type]) VALUES (N'PATIENT_RACE_COUNT_SHRINE_XML', N'patient_race_count_xml')
GO
INSERT [dbo].[SHRINE_Query_Result_Type_Mapping] ([i2b2_type], [SHRINE_type]) VALUES (N'PATIENT_SEX_COUNT_SHRINE_XML', N'patient_gender_count_xml')
GO
INSERT [dbo].[SHRINE_Query_Result_Type_Mapping] ([i2b2_type], [SHRINE_type]) VALUES (N'PATIENT_SITE_COUNT_SHRINE_XML', N'patient_count_xml')
GO
INSERT [dbo].[SHRINE_Query_Result_Type_Mapping] ([i2b2_type], [SHRINE_type]) VALUES (N'PATIENT_VITALSTATUS_COUNT_SHRINE_XML', N'patient_vitalstatus_count_xml')
GO
INSERT [dbo].[SHRINE_Query_Result_Type_Mapping] ([i2b2_type], [SHRINE_type]) VALUES (N'PATIENT_ZIP_COUNT_SHRINE_XML', N'patient_zip_count_xml')
GO
