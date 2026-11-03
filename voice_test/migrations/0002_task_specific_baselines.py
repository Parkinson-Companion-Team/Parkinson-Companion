import django.db.models.deletion
from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("voice_test", "0001_initial"),
    ]

    operations = [
        migrations.AddField(
            model_name="patientvoicebaseline",
            name="task_type",
            field=models.CharField(
                choices=[
                    ("vowel", "Sustained Vowel"),
                    ("ddk", "Diadochokinetic (pa-ta-ka)"),
                    ("reading", "Reading Passage"),
                ],
                default="vowel",
                max_length=10,
            ),
        ),
        migrations.AlterField(
            model_name="patientvoicebaseline",
            name="patient",
            field=models.ForeignKey(
                on_delete=django.db.models.deletion.CASCADE,
                related_name="voice_baselines",
                to="writing_test.patient",
            ),
        ),
        migrations.AddConstraint(
            model_name="patientvoicebaseline",
            constraint=models.UniqueConstraint(
                fields=("patient", "task_type"),
                name="unique_patient_voice_task_baseline",
            ),
        ),
    ]
