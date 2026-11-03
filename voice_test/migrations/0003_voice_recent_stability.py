from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("voice_test", "0002_task_specific_baselines"),
    ]

    operations = [
        migrations.AddField(
            model_name="voicetestresult",
            name="stability_vs_recent",
            field=models.FloatField(blank=True, null=True),
        ),
    ]
